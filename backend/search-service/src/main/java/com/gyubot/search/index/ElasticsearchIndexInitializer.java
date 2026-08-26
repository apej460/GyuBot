package com.gyubot.search.index;

import co.elastic.clients.elasticsearch._types.mapping.DenseVectorSimilarity;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.gyubot.search.embedding.EmbeddingClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;

/*
 * document-service가 시작할 때 MinIO 버킷 존재를 확인/생성하는 것과 같은 발상 — 검색용 인덱스가
 * 없으면 dense_vector 매핑을 포함해 미리 만들어둔다.
 */
@Component
public class ElasticsearchIndexInitializer {

    private final ElasticsearchClient client;
    private final String indexName;

    public ElasticsearchIndexInitializer(
            ElasticsearchClient client,
            @Value("${app.elasticsearch.document-chunk-index}") String indexName,
            EmbeddingClient embeddingClient) {
        this.client = client;
        this.indexName = indexName;
        ensureIndexExists(embeddingClient.dimensions());
    }

    private void ensureIndexExists(int dimensions) {
        try {
            boolean exists = client.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                return;
            }
            client.indices().create(c -> c
                    .index(indexName)
                    .mappings(m -> m
                            .properties("documentId", p -> p.long_(l -> l))
                            .properties("companyId", p -> p.long_(l -> l))
                            .properties("chunkIndex", p -> p.integer(i -> i))
                            .properties("text", p -> p.text(t -> t))
                            .properties("originalFilename", p -> p.keyword(k -> k))
                            .properties("contentType", p -> p.keyword(k -> k))
                            .properties("createdAt", p -> p.date(d -> d))
                            .properties("embedding", p -> p.denseVector(d -> d
                                    .dims(dimensions)
                                    .index(true)
                                    .similarity(DenseVectorSimilarity.Cosine)))));
        } catch (IOException e) {
            throw new UncheckedIOException("Elasticsearch 인덱스 초기화에 실패했습니다.", e);
        }
    }
}
