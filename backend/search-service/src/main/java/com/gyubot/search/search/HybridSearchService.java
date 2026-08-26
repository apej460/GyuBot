package com.gyubot.search.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
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

/*
 * BM25(키워드)와 kNN(벡터) 검색을 각각 실행한 뒤 Reciprocal Rank Fusion(RRF)으로 순위를 합친다.
 * ES 8.9+의 retriever(rrf) API로도 같은 걸 서버 쪽에서 할 수 있지만, 두 검색을 따로 호출해
 * 직접 합치는 쪽이 이해·검증하기 쉬워서 이렇게 구현했다 — 결과적으로 동일한 RRF 공식을 쓴다.
 */
@Service
public class HybridSearchService {

    // RRF 공식의 표준 상수. 순위가 낮은(멀리 있는) 결과의 영향을 완만하게 줄여준다.
    private static final int RRF_K = 60;
    private static final int CANDIDATE_MULTIPLIER = 10;

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
        List<Hit<DocumentChunk>> vectorHits = vectorSearch(companyId, queryText, topK);

        Map<String, Double> rrfScores = new LinkedHashMap<>();
        Map<String, DocumentChunk> chunksById = new LinkedHashMap<>();
        applyRrf(keywordHits, rrfScores, chunksById);
        applyRrf(vectorHits, rrfScores, chunksById);

        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> {
                    DocumentChunk chunk = chunksById.get(entry.getKey());
                    return new SearchResultResponse(
                            chunk.documentId(), chunk.chunkIndex(), chunk.text(), chunk.originalFilename(), entry.getValue());
                })
                .toList();
    }

    private void applyRrf(List<Hit<DocumentChunk>> hits, Map<String, Double> scores, Map<String, DocumentChunk> chunksById) {
        int rank = 1;
        for (Hit<DocumentChunk> hit : hits) {
            String id = hit.id();
            chunksById.putIfAbsent(id, hit.source());
            scores.merge(id, 1.0 / (RRF_K + rank), Double::sum);
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
                            .filter(f -> f.term(t -> t.field("companyId").value(companyId))))),
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
