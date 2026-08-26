package com.gyubot.chat.repository;

import com.gyubot.chat.domain.AnswerSource;

import java.util.List;

public interface AnswerSourceRepository {
    AnswerSource save(Long messageId, Long documentId, int chunkIndex, String originalFilename, String snippet, double score);

    List<AnswerSource> findAllByMessageId(Long messageId);
}
