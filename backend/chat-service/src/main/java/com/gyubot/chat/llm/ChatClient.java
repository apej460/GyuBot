package com.gyubot.chat.llm;

/*
 * RAG 답변을 생성하는 LLM 클라이언트. 지금은 OllamaChatClient(로컬, 무료)만 구현되어 있다.
 * 나중에 OpenAI/Anthropic API 키가 생기면 이 인터페이스를 구현하는 클래스를 하나 더 추가하고
 * 활성 빈만 바꾸면 된다 — search-service의 EmbeddingClient와 같은 구조.
 */
public interface ChatClient {

    String generate(String systemPrompt, String userPrompt);
}
