package com.gyubot.document.repository;

import com.gyubot.document.domain.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcDocumentRepository implements DocumentRepository {

    private static final String SELECT_COLUMNS =
            "id, company_id, title, original_filename, content_type, file_size, s3_key, "
                    + "version, category, effective_date, revision_date";

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
    public int countByCompanyId(Long companyId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM document WHERE company_id = ? AND deleted = FALSE", Integer.class, companyId);
        return count == null ? 0 : count;
    }

    @Override
    public Document save(
            Long companyId, String title, String originalFilename, String contentType, long fileSize, String s3Key,
            String version, String category, LocalDate effectiveDate, LocalDate revisionDate) {
        String sql = "INSERT INTO document(company_id, title, original_filename, content_type, file_size, s3_key, "
                + "version, category, effective_date, revision_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, companyId);
            ps.setString(2, title);
            ps.setString(3, originalFilename);
            ps.setString(4, contentType);
            ps.setLong(5, fileSize);
            ps.setString(6, s3Key);
            ps.setString(7, version);
            ps.setString(8, category);
            setDateOrNull(ps, 9, effectiveDate);
            setDateOrNull(ps, 10, revisionDate);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return new Document(
                key == null ? null : key.longValue(), companyId, title, originalFilename, contentType, fileSize, s3Key,
                version, category, effectiveDate, revisionDate);
    }

    @Override
    public void updateTitle(Long id, String title) {
        jdbcTemplate.update("UPDATE document SET title = ? WHERE id = ?", title, id);
    }

    @Override
    public void softDelete(Long id) {
        jdbcTemplate.update("UPDATE document SET deleted = TRUE WHERE id = ?", id);
    }

    private void setDateOrNull(PreparedStatement ps, int index, LocalDate date) throws SQLException {
        if (date == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setDate(index, Date.valueOf(date));
        }
    }

    private Document mapRow(ResultSet rs, int rowNum) throws SQLException {
        Date effectiveDate = rs.getDate("effective_date");
        Date revisionDate = rs.getDate("revision_date");
        return new Document(
                rs.getLong("id"),
                rs.getLong("company_id"),
                rs.getString("title"),
                rs.getString("original_filename"),
                rs.getString("content_type"),
                rs.getLong("file_size"),
                rs.getString("s3_key"),
                rs.getString("version"),
                rs.getString("category"),
                effectiveDate == null ? null : effectiveDate.toLocalDate(),
                revisionDate == null ? null : revisionDate.toLocalDate()
        );
    }
}
