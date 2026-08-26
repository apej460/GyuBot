package com.gyubot.chat.web;

import com.gyubot.chat.domain.ChatMessage;
import com.gyubot.chat.security.AuthPrincipal;
import com.gyubot.chat.service.ChatService;
import com.gyubot.chat.web.dto.AskRequest;
import com.gyubot.chat.web.dto.AskResponse;
import com.gyubot.chat.web.dto.ChatMessageResponse;
import com.gyubot.chat.web.dto.ChatSessionResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /*
     * 임직원·관리자 공통 - 질문 하나를 보내고 답변을 받는다. sessionId가 없으면 새 대화방을 만든다.
     */
    @PostMapping("/messages")
    public AskResponse ask(Authentication authentication, @Valid @RequestBody AskRequest request) {
        AuthPrincipal principal = principalOf(authentication);
        ChatService.ChatAnswer answer = chatService.ask(
                principal.companyId(), principal.userId(), request.sessionId(), request.question());
        return AskResponse.from(answer);
    }

    /*
     * 질의 이력 - 내 대화방 목록
     */
    @GetMapping("/sessions")
    public List<ChatSessionResponse> sessions(Authentication authentication) {
        AuthPrincipal principal = principalOf(authentication);
        return chatService.listSessions(principal.companyId(), principal.userId()).stream()
                .map(ChatSessionResponse::from)
                .toList();
    }

    /*
     * 질의 이력 - 대화방 하나의 전체 메시지(+근거 문서)
     */
    @GetMapping("/sessions/{id}/messages")
    public List<ChatMessageResponse> messages(Authentication authentication, @PathVariable Long id) {
        AuthPrincipal principal = principalOf(authentication);
        List<ChatMessage> messages = chatService.listMessages(id, principal.companyId(), principal.userId());
        return messages.stream()
                .map(message -> ChatMessageResponse.from(message, chatService.sourcesOf(message.id())))
                .toList();
    }

    private AuthPrincipal principalOf(Authentication authentication) {
        return (AuthPrincipal) authentication.getPrincipal();
    }
}
