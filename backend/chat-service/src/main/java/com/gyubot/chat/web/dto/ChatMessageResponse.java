package com.gyubot.chat.web.dto;

import com.gyubot.chat.domain.AnswerSource;
import com.gyubot.chat.domain.ChatMessage;

import java.time.Instant;
import java.util.List;

public record ChatMessageResponse(
        Long id,
        String role,
        String content,
        Instant createdAt,
        List<AnswerSourceResponse> sources
) {
    public static ChatMessageResponse from(ChatMessage message, List<AnswerSource> sources) {
        return new ChatMessageResponse(
                message.id(),
                message.role().name(),
                message.content(),
                message.createdAt(),
                sources.stream().map(AnswerSourceResponse::from).toList());
    }
}
