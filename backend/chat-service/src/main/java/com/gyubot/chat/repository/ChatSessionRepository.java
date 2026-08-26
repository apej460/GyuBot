package com.gyubot.chat.repository;

import com.gyubot.chat.domain.ChatSession;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository {
    ChatSession save(Long companyId, Long userId, String title);

    Optional<ChatSession> findById(Long id);

    List<ChatSession> findAllByCompanyIdAndUserId(Long companyId, Long userId);
}
