package com.gyubot.chat.web.dto;

import com.gyubot.chat.service.ChatStatsService;

import java.util.List;

public record ChatStatsResponse(
        int todayCount,
        int unansweredCount,
        List<DailyCountResponse> dailyLast7,
        List<KeywordCountResponse> topKeywords
) {
    public static ChatStatsResponse from(ChatStatsService.DashboardStats stats) {
        List<DailyCountResponse> daily = stats.dailyLast7().entrySet().stream()
                .map(e -> new DailyCountResponse(e.getKey(), e.getValue()))
                .toList();
        List<KeywordCountResponse> keywords = stats.topKeywords().stream()
                .map(k -> new KeywordCountResponse(k.keyword(), k.count()))
                .toList();
        return new ChatStatsResponse(stats.todayCount(), stats.unansweredCount(), daily, keywords);
    }
}
