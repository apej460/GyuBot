package com.gyubot.chat.repository;

import java.time.LocalDate;
import java.util.List;

public interface ChatStatsRepository {
    int countTodayQuestions(Long companyId);

    int countUnanswered(Long companyId);

    List<DailyCount> dailyQuestionCounts(Long companyId, int days);

    List<String> recentQuestionContents(Long companyId, int limit);

    record DailyCount(LocalDate date, int count) {
    }
}
