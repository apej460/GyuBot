package com.gyubot.search.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.gyubot.search.client.DocumentServiceClient;
import com.gyubot.search.chunk.TextChunker;
import com.gyubot.search.embedding.EmbeddingClient;
import com.gyubot.search.event.DocumentEvent;
import com.gyubot.search.extract.TextExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;

/*
 * document.uploaded 이벤트 하나를 받아 원문 다운로드 → 텍스트 추출 → Chunking → 임베딩 → ES 색인까지
 * 전부 처리한다. document.deleted는 해당 documentId의 청크를 전부 지운다.
 */
@Service
public class DocumentChunkIndexer {

    private static final Logger log = LoggerFactory.getLogger(DocumentChunkIndexer.class);

    private final DocumentServiceClient documentServiceClient;
    private final TextExtractor textExtractor;
    private final TextChunker textChunker;
    private final EmbeddingClient embeddingClient;
    private final ElasticsearchClient elasticsearchClient;
    private final String indexName;

    public DocumentChunkIndexer(
            DocumentServiceClient documentServiceClient,
            TextExtractor textExtractor,
            TextChunker textChunker,
            EmbeddingClient embeddingClient,
            ElasticsearchClient elasticsearchClient,
            @Value("${app.elasticsearch.document-chunk-index}") String indexName) {
        this.documentServiceClient = documentServiceClient;
        this.textExtractor = textExtractor;
        this.textChunker = textChunker;
        this.embeddingClient = embeddingClient;
        this.elasticsearchClient = elasticsearchClient;
        this.indexName = indexName;
    }

    public void index(DocumentEvent event) {
        // 재색인(재업로드 없이 재처리)해도 같은 documentId의 이전 청크를 먼저 지워, 청크 개수가 줄어든
        // 경우에도 오래된 청크가 인덱스에 남지 않게 한다.
        deleteByDocumentId(event.documentId());

        byte[] content = documentServiceClient.download(event.documentId());
        String text = textExtractor.extract(event.originalFilename(), content);
        List<String> chunks = textChunker.chunk(text);

        if (chunks.isEmpty()) {
            log.warn("문서 {}에서 추출된 텍스트가 없어 색인을 건너뜁니다.", event.documentId());
            return;
        }

        List<BulkOperation> operations = new java.util.ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);
            DocumentChunk chunk = new DocumentChunk(
                    event.documentId(),
                    event.companyId(),
                    i,
                    chunkText,
                    toList(embeddingClient.embed(chunkText)),
                    event.originalFilename(),
                    event.contentType(),
                    Instant.now());
            operations.add(BulkOperation.of(b -> b.index(idx -> idx.index(indexName).id(chunk.id()).document(chunk))));
        }

        bulkIndex(operations);
        log.info("문서 {} 색인 완료 — 청크 {}개", event.documentId(), chunks.size());
    }

    public void deleteByDocumentId(Long documentId) {
        try {
            elasticsearchClient.deleteByQuery(d -> d
                    .index(indexName)
                    .query(q -> q.term(t -> t.field("documentId").value(documentId)))
                    .refresh(true));
        } catch (IOException e) {
            throw new UncheckedIOException("Elasticsearch 청크 삭제에 실패했습니다.", e);
        }
    }

    private List<Float> toList(float[] values) {
        List<Float> list = new java.util.ArrayList<>(values.length);
        for (float v : values) {
            list.add(v);
        }
        return list;
    }

    private void bulkIndex(List<BulkOperation> operations) {
        try {
            elasticsearchClient.bulk(BulkRequest.of(b -> b.operations(operations).refresh(
                    co.elastic.clients.elasticsearch._types.Refresh.True)));
        } catch (IOException e) {
            throw new UncheckedIOException("Elasticsearch 색인에 실패했습니다.", e);
        }
    }
}
