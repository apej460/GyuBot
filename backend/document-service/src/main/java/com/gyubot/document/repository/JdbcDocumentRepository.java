package com.gyubot.document.repository;

import com.gyubot.document.domain.Document;
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
public class JdbcDocumentRepository implements DocumentRepository {

    private static final String SELECT_COLUMNS =
            "id, company_id, title, original_filename, content_type, file_size, s3_key";

    private final JdbcTemplate jdbcTemplate;

    public JdbcDocumentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Document> findById(Long id) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM document WHERE id = ? AND deleted = FALSE",
                this::mapRow,
                id
        ).stream().findFirst();
    }

    @Override
    public List<Document> findAllByCompanyId(Long companyId) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM document WHERE company_id = ? AND deleted = FALSE ORDER BY id DESC",
                this::mapRow,
                companyId
        );
    }

    @Override
    public Document save(Long companyId, String title, String originalFilename, String contentType, long fileSize, String s3Key) {
        String sql = "INSERT INTO document(company_id, title, original_filename, content_type, file_size, s3_key) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, companyId);
            ps.setString(2, title);
            ps.setString(3, originalFilename);
            ps.setString(4, contentType);
            ps.setLong(5, fileSize);
            ps.setString(6, s3Key);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return new Document(key == null ? null : key.longValue(), companyId, title, originalFilename, contentType, fileSize, s3Key);
    }

    @Override
    public void updateTitle(Long id, String title) {
        jdbcTemplate.update("UPDATE document SET title = ? WHERE id = ?", title, id);
    }

    @Override
    public void softDelete(Long id) {
        jdbcTemplate.update("UPDATE document SET deleted = TRUE WHERE id = ?", id);
    }

    private Document mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Document(
                rs.getLong("id"),
                rs.getLong("company_id"),
                rs.getString("title"),
                rs.getString("original_filename"),
                rs.getString("content_type"),
                rs.getLong("file_size"),
                rs.getString("s3_key")
        );
    }
}
