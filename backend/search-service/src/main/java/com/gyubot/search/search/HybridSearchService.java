package com.gyubot.search.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.util.NamedValue;
import com.gyubot.search.embedding.EmbeddingClient;
import com.gyubot.search.index.DocumentChunk;
import com.gyubot.search.search.dto.SearchResultResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * BM25(키워드)와 kNN(벡터) 검색을 각각 실행한 뒤 Reciprocal Rank Fusion(RRF)으로 순위를 합친다.
 * ES 8.9+의 retriever(rrf) API로도 같은 걸 서버 쪽에서 할 수 있지만, 두 검색을 따로 호출해
 * 직접 합치는 쪽이 이해·검증하기 쉬워서 이렇게 구현했다 — 결과적으로 동일한 RRF 공식을 쓴다.
 *
 * kNN은 실제 유사도가 낮아도 topK를 항상 채워서 반환한다 — 짧고 문체가 비슷한 사내 규정
 * 문서끼리는 nomic-embed-text의 원시 코사인 유사도가 관련 없는 질문에도 관련 있는 질문과
 * 거의 같거나(심지어 더 높게) 나와서, 벡터 유사도만으로는 "진짜 관련 있는지"를 걸러낼 수 없다는
 * 걸 실측으로 확인했다(예: "화성 여행 경비는?" 질문이 "휴가 규정" 문서에 대해 실제 휴가 질문보다
 * 더 높은 유사도 점수를 받음). 그래서 벡터 검색은 키워드로 이미 매칭된 청크의 순위를 보정하는
 * 용도로만 쓰고, 키워드 매칭이 전혀 없는 질문·청크는 최종 결과에서 제외한다.
 */
@Service
public class HybridSearchService {

    // RRF 공식의 표준 상수. 순위가 낮은(멀리 있는) 결과의 영향을 완만하게 줄여준다.
    private static final int RRF_K = 60;
    private static final int CANDIDATE_MULTIPLIER = 10;
    // 벡터 branch를 키워드와 동일한 가중치(1.0)로 섞으면, 이 코퍼스에서 거의 모든 질문에 대해 벡터
    // 순위가 유독 높게 나오는 특정 문서(원인 불명의 임베딩 공간 쏠림)가 실제로 더 정확히 매칭된
    // 키워드 결과를 순위에서 밀어낼 수 있다는 걸 확인해서, 키워드보다 낮은 가중치로 보정 정도로만 반영한다.
    private static final double VECTOR_WEIGHT = 0.3;

    // 인용 시 매칭된 부분을 표시하기 위한 하이라이트 마커. 실제 문서 텍스트에 나타날 일이 없는
    // 제어 문자를 써서, 프론트엔드가 이 문자만 <mark> 태그로 바꿔치기하면 나머지는 그대로 이스케이프해도
    // 안전하다(XSS 방지). chat-service의 ChatService가 LLM 프롬프트에 넣기 전 이 마커를 제거한다 —
    // 두 서비스가 이 두 문자에 대해 암묵적으로 합의하고 있으므로, 바꾸려면 양쪽 다 같이 바꿔야 한다.
    public static final String HIGHLIGHT_START = "\u0001";
    public static final String HIGHLIGHT_END = "\u0002";

    private final ElasticsearchClient client;
    private final EmbeddingClient embeddingClient;
    private final String indexName;

    public HybridSearchService(
            ElasticsearchClient client,
            EmbeddingClient embeddingClient,
            @Value("${app.elasticsearch.document-chunk-index}") String indexName) {
        this.client = client;
        this.embeddingClient = embeddingClient;
        this.indexName = indexName;
    }

    public List<SearchResultResponse> search(Long companyId, String queryText, int topK) {
        List<Hit<DocumentChunk>> keywordHits = keywordSearch(companyId, queryText, topK);
        if (keywordHits.isEmpty()) {
            // 키워드로 전혀 안 걸리면 벡터만으로는 관련성을 신뢰할 수 없다(클래스 주석 참고) — 바로 빈 결과.
            return List.of();
        }
        List<Hit<DocumentChunk>> vectorHits = vectorSearch(companyId, queryText, topK);

        Set<String> keywordMatchedIds = keywordHits.stream().map(Hit::id).collect(Collectors.toSet());
        Map<String, String> highlightedTextById = extractHighlights(keywordHits);

        Map<String, Double> rrfScores = new LinkedHashMap<>();
        Map<String, DocumentChunk> chunksById = new LinkedHashMap<>();
        applyRrf(keywordHits, 1.0, rrfScores, chunksById);
        applyRrf(vectorHits, VECTOR_WEIGHT, rrfScores, chunksById);

        return rrfScores.entrySet().stream()
                // 벡터 검색은 키워드로 이미 찾은 청크의 순위 보정에만 쓰고, 벡터 단독으로 새 청크를 들여오지 않는다.
                .filter(entry -> keywordMatchedIds.contains(entry.getKey()))
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> {
                    DocumentChunk chunk = chunksById.get(entry.getKey());
                    String text = highlightedTextById.getOrDefault(entry.getKey(), chunk.text());
                    return new SearchResultResponse(
                            chunk.documentId(), chunk.chunkIndex(), text, chunk.originalFilename(), entry.getValue());
                })
                .toList();
    }

    private Map<String, String> extractHighlights(List<Hit<DocumentChunk>> keywordHits) {
        Map<String, String> highlights = new LinkedHashMap<>();
        for (Hit<DocumentChunk> hit : keywordHits) {
            List<String> fragments = hit.highlight().get("text");
            if (fragments != null && !fragments.isEmpty()) {
                highlights.put(hit.id(), fragments.get(0));
            }
        }
        return highlights;
    }

    private void applyRrf(List<Hit<DocumentChunk>> hits, double weight, Map<String, Double> scores, Map<String, DocumentChunk> chunksById) {
        int rank = 1;
        for (Hit<DocumentChunk> hit : hits) {
            String id = hit.id();
            chunksById.putIfAbsent(id, hit.source());
            scores.merge(id, weight / (RRF_K + rank), Double::sum);
            rank++;
        }
    }

    private List<Hit<DocumentChunk>> keywordSearch(Long companyId, String queryText, int topK) {
        try {
            SearchResponse<DocumentChunk> response = client.search(s -> s
                    .index(indexName)
                    .size(topK)
                    .query(q -> q.bool(b -> b
                            .must(m -> m.match(mt -> mt.field("text").query(queryText)))
                            .filter(f -> f.term(t -> t.field("companyId").value(companyId)))))
                    // numberOfFragments(0): 조각이 아니라 필드 전체 텍스트를 매칭 부분만 마커로 감싸서 돌려준다
                    // — 청크가 원래 전체가 인용문(snippet)으로 쓰이는 구조라 이렇게 받는 게 맞다.
                    .highlight(h -> h.fields(NamedValue.of("text", HighlightField.of(f -> f
                            .preTags(HIGHLIGHT_START)
                            .postTags(HIGHLIGHT_END)
                            .numberOfFragments(0))))),
                    DocumentChunk.class);
            return response.hits().hits();
        } catch (IOException e) {
            throw new UncheckedIOException("Elasticsearch 키워드 검색에 실패했습니다.", e);
        }
    }

    private List<Hit<DocumentChunk>> vectorSearch(Long companyId, String queryText, int topK) {
        List<Float> queryVector = toList(embeddingClient.embed(queryText));
        try {
            SearchResponse<DocumentChunk> response = client.search(s -> s
                    .index(indexName)
                    .knn(k -> k
                            .field("embedding")
                            .queryVector(queryVector)
                            .k(topK)
                            .numCandidates(topK * CANDIDATE_MULTIPLIER)
                            .filter(f -> f.term(t -> t.field("companyId").value(companyId))))
                    .size(topK),
                    DocumentChunk.class);
            return response.hits().hits();
        } catch (IOException e) {
            throw new UncheckedIOException("Elasticsearch 벡터 검색에 실패했습니다.", e);
        }
    }

    private List<Float> toList(float[] values) {
        List<Float> list = new ArrayList<>(values.length);
        for (float v : values) {
            list.add(v);
        }
        return list;
    }
}
