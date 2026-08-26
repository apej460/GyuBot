package com.gyubot.chat.repository;

import com.gyubot.chat.domain.ChatMessage;
import com.gyubot.chat.domain.MessageRole;

import java.util.List;

public interface ChatMessageRepository {
    ChatMessage save(Long sessionId, MessageRole role, String content);

    List<ChatMessage> findAllBySessionId(Long sessionId);
}
