package com.gyubot.chat.web.dto;

import com.gyubot.chat.domain.AnswerSource;

public record AnswerSourceResponse(
        Long documentId,
        int chunkIndex,
        String originalFilename,
        String snippet,
        double score
) {
    public static AnswerSourceResponse from(AnswerSource source) {
        return new AnswerSourceResponse(
                source.documentId(), source.chunkIndex(), source.originalFilename(), source.snippet(), source.score());
    }
}
