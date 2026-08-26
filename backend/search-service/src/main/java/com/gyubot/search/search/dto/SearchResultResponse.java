package com.gyubot.search.search.dto;

public record SearchResultResponse(
        Long documentId,
        int chunkIndex,
        String text,
        String originalFilename,
        double score
) {
}
