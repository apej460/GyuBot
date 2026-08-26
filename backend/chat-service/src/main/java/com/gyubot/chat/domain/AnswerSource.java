package com.gyubot.chat.domain;

/*
 * AI 답변의 근거가 된 문서 청크. REQ-F-007(근거 표시)을 위해 답변마다 몇 개씩 남는다.
 */
public record AnswerSource(
        Long id,
        Long messageId,
        Long documentId,
        int chunkIndex,
        String originalFilename,
        String snippet,
        double score
) {
}
