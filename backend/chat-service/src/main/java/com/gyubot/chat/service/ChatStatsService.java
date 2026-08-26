package com.gyubot.chat.service;

import com.gyubot.chat.repository.ChatStatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatStatsService {

    private static final int DAILY_WINDOW = 7;
    private static final int KEYWORD_SAMPLE_SIZE = 200;
    private static final int TOP_KEYWORD_COUNT = 4;

    private final ChatStatsRepository chatStatsRepository;
    private final KeywordExtractor keywordExtractor;

    public ChatStatsService(ChatStatsRepository chatStatsRepository, KeywordExtractor keywordExtractor) {
        this.chatStatsRepository = chatStatsRepository;
        this.keywordExtractor = keywordExtractor;
    }

    @Transactional(readOnly = true)
    public DashboardStats forCompany(Long companyId) {
        int todayCount = chatStatsRepository.countTodayQuestions(companyId);
        int unansweredCount = chatStatsRepository.countUnanswered(companyId);
        Map<LocalDate, Integer> daily = fillMissingDays(chatStatsRepository.dailyQuestionCounts(companyId, DAILY_WINDOW));
        List<String> recentQuestions = chatStatsRepository.recentQuestionContents(companyId, KEYWORD_SAMPLE_SIZE);
        List<KeywordExtractor.KeywordCount> topKeywords = keywordExtractor.topKeywords(recentQuestions, TOP_KEYWORD_COUNT);
        return new DashboardStats(todayCount, unansweredCount, daily, topKeywords);
    }

    // 질문이 없었던 날도 0으로 채워서 최근 7일 전체를 그래프에 그릴 수 있게 한다.
    private Map<LocalDate, Integer> fillMissingDays(List<ChatStatsRepository.DailyCount> rows) {
        Map<LocalDate, Integer> byDate = new LinkedHashMap<>();
        for (ChatStatsRepository.DailyCount row : rows) {
            byDate.put(row.date(), row.count());
        }
        Map<LocalDate, Integer> filled = new LinkedHashMap<>();
        LocalDate start = LocalDate.now().minusDays(DAILY_WINDOW - 1L);
        for (int i = 0; i < DAILY_WINDOW; i++) {
            LocalDate day = start.plusDays(i);
            filled.put(day, byDate.getOrDefault(day, 0));
        }
        return filled;
    }

    public record DashboardStats(
            int todayCount,
            int unansweredCount,
            Map<LocalDate, Integer> dailyLast7,
            List<KeywordExtractor.KeywordCount> topKeywords) {
    }
}
