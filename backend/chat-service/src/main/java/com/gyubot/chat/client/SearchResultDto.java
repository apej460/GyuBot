package com.gyubot.chat.client;

/*
 * search-service의 GET /internal/search 응답 하나의 구조. 필드가 search-service의
 * SearchResultResponse와 정확히 일치해야 한다.
 */
public record SearchResultDto(
        Long documentId,
        int chunkIndex,
        String text,
        String originalFilename,
        double score
) {
}
