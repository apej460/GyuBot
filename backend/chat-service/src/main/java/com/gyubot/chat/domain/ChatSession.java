package com.gyubot.chat.domain;

import java.time.Instant;

public record ChatSession(Long id, Long companyId, Long userId, String title, Instant createdAt) {
}
