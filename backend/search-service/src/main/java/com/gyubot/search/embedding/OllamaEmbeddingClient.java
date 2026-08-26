package com.gyubot.search.embedding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OllamaEmbeddingClient implements EmbeddingClient {

    // nomic-embed-text가 반환하는 벡터 차원. 모델을 바꾸면 이 값과 ES 인덱스의 dense_vector 매핑도 같이 바꿔야 한다.
    private static final int DIMENSIONS = 768;

    private final RestClient restClient;
    private final String model;

    public OllamaEmbeddingClient(
            @Value("${app.embedding.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${app.embedding.ollama.model:nomic-embed-text}") String model) {
        this.restClient = RestClient.create(baseUrl);
        this.model = model;
    }

    @Override
    public float[] embed(String text) {
        EmbeddingResponse response = restClient.post()
                .uri("/api/embeddings")
                .body(new EmbeddingRequest(model, text))
                .retrieve()
                .body(EmbeddingResponse.class);

        double[] embedding = response.embedding();
        float[] result = new float[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            result[i] = (float) embedding[i];
        }
        return result;
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    private record EmbeddingRequest(String model, String prompt) {
    }

    private record EmbeddingResponse(double[] embedding) {
    }
}
