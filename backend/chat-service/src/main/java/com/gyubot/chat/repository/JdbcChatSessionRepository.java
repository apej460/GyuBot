package com.gyubot.chat.repository;

import com.gyubot.chat.domain.ChatSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcChatSessionRepository implements ChatSessionRepository {

    private static final String SELECT_COLUMNS = "id, company_id, user_id, title, created_at";

    private final JdbcTemplate jdbcTemplate;

    public JdbcChatSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ChatSession save(Long companyId, Long userId, String title) {
        String sql = "INSERT INTO chat_session(company_id, user_id, title) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, companyId);
            ps.setLong(2, userId);
            ps.setString(3, title);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return findById(key.longValue()).orElseThrow();
    }

    @Override
    public Optional<ChatSession> findById(Long id) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM chat_session WHERE id = ?",
                this::mapRow,
                id
        ).stream().findFirst();
    }

    @Override
    public List<ChatSession> findAllByCompanyIdAndUserId(Long companyId, Long userId) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM chat_session WHERE company_id = ? AND user_id = ? ORDER BY id DESC",
                this::mapRow,
                companyId, userId
        );
    }

    private ChatSession mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new ChatSession(
                rs.getLong("id"),
                rs.getLong("company_id"),
                rs.getLong("user_id"),
                rs.getString("title"),
                createdAt == null ? null : createdAt.toInstant());
    }
}
