package com.gyubot.chat.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Repository
public class JdbcChatStatsRepository implements ChatStatsRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcChatStatsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int countTodayQuestions(Long companyId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_message cm JOIN chat_session cs ON cm.session_id = cs.id "
                        + "WHERE cs.company_id = ? AND cm.role = 'USER' AND DATE(cm.created_at) = CURDATE()",
                Integer.class, companyId);
        return count == null ? 0 : count;
    }

    @Override
    public int countUnanswered(Long companyId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_message cm JOIN chat_session cs ON cm.session_id = cs.id "
                        + "WHERE cs.company_id = ? AND cm.role = 'ASSISTANT' "
                        + "AND NOT EXISTS (SELECT 1 FROM answer_source a WHERE a.message_id = cm.id)",
                Integer.class, companyId);
        return count == null ? 0 : count;
    }

    @Override
    public List<DailyCount> dailyQuestionCounts(Long companyId, int days) {
        return jdbcTemplate.query(
                "SELECT DATE(cm.created_at) AS d, COUNT(*) AS c FROM chat_message cm "
                        + "JOIN chat_session cs ON cm.session_id = cs.id "
                        + "WHERE cs.company_id = ? AND cm.role = 'USER' "
                        + "AND cm.created_at >= (CURDATE() - INTERVAL ? DAY) "
                        + "GROUP BY DATE(cm.created_at) ORDER BY d",
                (rs, rowNum) -> new DailyCount(((Date) rs.getDate("d")).toLocalDate(), rs.getInt("c")),
                companyId, days - 1);
    }

    @Override
    public List<String> recentQuestionContents(Long companyId, int limit) {
        return jdbcTemplate.query(
                "SELECT cm.content FROM chat_message cm JOIN chat_session cs ON cm.session_id = cs.id "
                        + "WHERE cs.company_id = ? AND cm.role = 'USER' ORDER BY cm.id DESC LIMIT ?",
                (rs, rowNum) -> rs.getString("content"),
                companyId, limit);
    }
}
