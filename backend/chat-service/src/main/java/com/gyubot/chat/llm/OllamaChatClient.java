package com.gyubot.chat.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class OllamaChatClient implements ChatClient {

    private final RestClient restClient;
    private final String model;

    public OllamaChatClient(
            @Value("${app.llm.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${app.llm.ollama.model:qwen2.5:7b}") String model) {
        // 로컬 CPU 추론은 첫 호출(모델 로딩 포함)에 특히 느릴 수 있어 타임아웃을 넉넉하게 둔다.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setReadTimeout(180_000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.model = model;
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        OllamaChatRequest request = new OllamaChatRequest(
                model,
                List.of(
                        new OllamaMessage("system", systemPrompt),
                        new OllamaMessage("user", userPrompt)),
                false);

        OllamaChatResponse response = restClient.post()
                .uri("/api/chat")
                .body(request)
                .retrieve()
                .body(OllamaChatResponse.class);

        return response.message().content();
    }

    private record OllamaMessage(String role, String content) {
    }

    private record OllamaChatRequest(String model, List<OllamaMessage> messages, boolean stream) {
    }

    private record OllamaChatResponse(OllamaMessage message) {
    }
}
