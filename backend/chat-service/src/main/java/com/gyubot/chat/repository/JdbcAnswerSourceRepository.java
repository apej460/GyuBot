package com.gyubot.chat.repository;

import com.gyubot.chat.domain.AnswerSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcAnswerSourceRepository implements AnswerSourceRepository {

    private static final String SELECT_COLUMNS =
            "id, message_id, document_id, chunk_index, original_filename, snippet, score";

    private final JdbcTemplate jdbcTemplate;

    public JdbcAnswerSourceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AnswerSource save(Long messageId, Long documentId, int chunkIndex, String originalFilename, String snippet, double score) {
        String sql = "INSERT INTO answer_source(message_id, document_id, chunk_index, original_filename, snippet, score) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, messageId);
            ps.setLong(2, documentId);
            ps.setInt(3, chunkIndex);
            ps.setString(4, originalFilename);
            ps.setString(5, snippet);
            ps.setDouble(6, score);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return findById(key.longValue()).orElseThrow();
    }

    @Override
    public List<AnswerSource> findAllByMessageId(Long messageId) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM answer_source WHERE message_id = ? ORDER BY score DESC",
                this::mapRow,
                messageId
        );
    }

    private Optional<AnswerSource> findById(Long id) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM answer_source WHERE id = ?",
                this::mapRow,
                id
        ).stream().findFirst();
    }

    private AnswerSource mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new AnswerSource(
                rs.getLong("id"),
                rs.getLong("message_id"),
                rs.getLong("document_id"),
                rs.getInt("chunk_index"),
                rs.getString("original_filename"),
                rs.getString("snippet"),
                rs.getDouble("score"));
    }
}
