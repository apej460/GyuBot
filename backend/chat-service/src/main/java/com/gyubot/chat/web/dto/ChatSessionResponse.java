package com.gyubot.chat.web.dto;

import com.gyubot.chat.domain.ChatSession;

import java.time.Instant;

public record ChatSessionResponse(Long id, String title, Instant createdAt) {
    public static ChatSessionResponse from(ChatSession session) {
        return new ChatSessionResponse(session.id(), session.title(), session.createdAt());
    }
}
