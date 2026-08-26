package com.gyubot.search.embedding;

/*
 * 텍스트를 벡터로 바꾸는 임베딩 모델 클라이언트. 지금은 OllamaEmbeddingClient(로컬, 무료)만
 * 구현되어 있다. 나중에 OpenAI API 키가 생기면 이 인터페이스를 구현하는 클래스를 하나 더
 * 추가하고 활성 빈만 바꾸면 된다 — Chunking/색인/검색 코드는 이 인터페이스만 알고 있어서
 * 구현체를 바꿔도 건드릴 필요가 없다. dimensions()가 바뀌면 ES 인덱스 매핑도 새로 만들어야 한다.
 */
public interface EmbeddingClient {

    float[] embed(String text);

    int dimensions();
}
