package com.gyubot.chat.service;

import com.gyubot.chat.client.SearchResultDto;
import com.gyubot.chat.client.SearchServiceClient;
import com.gyubot.chat.domain.AnswerSource;
import com.gyubot.chat.domain.ChatMessage;
import com.gyubot.chat.domain.ChatSession;
import com.gyubot.chat.domain.MessageRole;
import com.gyubot.chat.exception.ChatSessionNotFoundException;
import com.gyubot.chat.llm.ChatClient;
import com.gyubot.chat.repository.AnswerSourceRepository;
import com.gyubot.chat.repository.ChatMessageRepository;
import com.gyubot.chat.repository.ChatSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatService {

    // REQ-F-007: 답변엔 근거 문서를 함께 보여줘야 하므로, 검색 결과 상위 몇 개를 그대로 근거로 남긴다.
    private static final int TOP_K = 5;
    private static final int TITLE_MAX_LENGTH = 40;

    // search-service의 HybridSearchService가 인용 하이라이트를 표시하려고 청크 텍스트에 심어 보내는
    // 마커. answer_source.snippet에는 그대로 저장해 프론트가 <mark>로 바꿔 쓰지만, LLM 프롬프트에는
    // 불필요한 제어 문자를 보낼 이유가 없어 여기서만 제거한다. 두 서비스가 이 문자에 대해 암묵적으로
    // 합의하고 있으므로, search-service의 HybridSearchService.HIGHLIGHT_START/END를 바꾸면 여기도 같이 바꿔야 한다.
    private static final String HIGHLIGHT_START = "\u0001";
    private static final String HIGHLIGHT_END = "\u0002";

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final AnswerSourceRepository answerSourceRepository;
    private final SearchServiceClient searchServiceClient;
    private final ChatClient chatClient;

    public ChatService(
            ChatSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            AnswerSourceRepository answerSourceRepository,
            SearchServiceClient searchServiceClient,
            ChatClient chatClient) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.answerSourceRepository = answerSourceRepository;
        this.searchServiceClient = searchServiceClient;
        this.chatClient = chatClient;
    }

    @Transactional
    public ChatAnswer ask(Long companyId, Long userId, Long sessionId, String question) {
        ChatSession session = sessionId != null
                ? requireOwnedSession(sessionId, companyId, userId)
                : sessionRepository.save(companyId, userId, titleFrom(question));

        messageRepository.save(session.id(), MessageRole.USER, question);

        List<SearchResultDto> results = searchServiceClient.search(companyId, question, TOP_K);

        String answerText = results.isEmpty()
                ? "관련된 사내 규정을 찾지 못했습니다. 질문을 조금 더 구체적으로 입력해주세요."
                : chatClient.generate(buildSystemPrompt(results), question);

        ChatMessage assistantMessage = messageRepository.save(session.id(), MessageRole.ASSISTANT, answerText);

        List<AnswerSource> sources = results.stream()
                .map(r -> answerSourceRepository.save(
                        assistantMessage.id(), r.documentId(), r.chunkIndex(), r.originalFilename(), r.text(), r.score()))
                .toList();

        return new ChatAnswer(session.id(), assistantMessage.id(), answerText, sources);
    }

    @Transactional(readOnly = true)
    public List<ChatSession> listSessions(Long companyId, Long userId) {
        return sessionRepository.findAllByCompanyIdAndUserId(companyId, userId);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> listMessages(Long sessionId, Long companyId, Long userId) {
        requireOwnedSession(sessionId, companyId, userId);
        return messageRepository.findAllBySessionId(sessionId);
    }

    @Transactional(readOnly = true)
    public List<AnswerSource> sourcesOf(Long messageId) {
        return answerSourceRepository.findAllByMessageId(messageId);
    }

    private String buildSystemPrompt(List<SearchResultDto> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 사내 규정을 안내하는 챗봇입니다. 아래 [참고 문서]에 있는 내용만 근거로 한국어로 답변하세요. ")
                .append("참고 문서에 없는 내용은 추측하거나 지어내지 말고, 답을 찾을 수 없으면 모른다고 답하세요.\n\n[참고 문서]\n");
        for (SearchResultDto r : results) {
            sb.append("- (").append(r.originalFilename()).append(") ").append(stripHighlightMarkers(r.text())).append('\n');
        }
        return sb.toString();
    }

    private String stripHighlightMarkers(String text) {
        return text.replace(HIGHLIGHT_START, "").replace(HIGHLIGHT_END, "");
    }

    private String titleFrom(String question) {
        return question.length() > TITLE_MAX_LENGTH ? question.substring(0, TITLE_MAX_LENGTH) + "…" : question;
    }

    private ChatSession requireOwnedSession(Long sessionId, Long companyId, Long userId) {
        ChatSession session = sessionRepository.findById(sessionId).orElseThrow(ChatSessionNotFoundException::new);
        // 다른 사람/다른 회사의 세션이면 403이 아니라 404로 존재 자체를 숨긴다 (테넌트 격리, REQ-F-018).
        if (!session.companyId().equals(companyId) || !session.userId().equals(userId)) {
            throw new ChatSessionNotFoundException();
        }
        return session;
    }

    public record ChatAnswer(Long sessionId, Long messageId, String answer, List<AnswerSource> sources) {
    }
}
