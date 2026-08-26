package com.gyubot.chat.domain;

import java.time.Instant;

public record ChatMessage(Long id, Long sessionId, MessageRole role, String content, Instant createdAt) {
}
