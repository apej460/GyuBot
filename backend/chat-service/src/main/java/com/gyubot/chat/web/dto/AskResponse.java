package com.gyubot.chat.web.dto;

import com.gyubot.chat.service.ChatService;

import java.util.List;

public record AskResponse(Long sessionId, Long messageId, String answer, List<AnswerSourceResponse> sources) {
    public static AskResponse from(ChatService.ChatAnswer answer) {
        return new AskResponse(
                answer.sessionId(),
                answer.messageId(),
                answer.answer(),
                answer.sources().stream().map(AnswerSourceResponse::from).toList());
    }
}
