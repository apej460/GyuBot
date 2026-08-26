package com.gyubot.chat.repository;

import com.gyubot.chat.domain.ChatMessage;
import com.gyubot.chat.domain.MessageRole;
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
public class JdbcChatMessageRepository implements ChatMessageRepository {

    private static final String SELECT_COLUMNS = "id, session_id, role, content, created_at";

    private final JdbcTemplate jdbcTemplate;

    public JdbcChatMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ChatMessage save(Long sessionId, MessageRole role, String content) {
        String sql = "INSERT INTO chat_message(session_id, role, content) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, sessionId);
            ps.setString(2, role.name());
            ps.setString(3, content);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return findById(key.longValue()).orElseThrow();
    }

    @Override
    public List<ChatMessage> findAllBySessionId(Long sessionId) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM chat_message WHERE session_id = ? ORDER BY id ASC",
                this::mapRow,
                sessionId
        );
    }

    private Optional<ChatMessage> findById(Long id) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM chat_message WHERE id = ?",
                this::mapRow,
                id
        ).stream().findFirst();
    }

    private ChatMessage mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new ChatMessage(
                rs.getLong("id"),
                rs.getLong("session_id"),
                MessageRole.valueOf(rs.getString("role")),
                rs.getString("content"),
                createdAt == null ? null : createdAt.toInstant());
    }
}
