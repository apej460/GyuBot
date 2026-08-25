package com.gyubot.user.repository;

import com.gyubot.user.domain.SignupRequest;
import com.gyubot.user.domain.SignupStatus;
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
public class JdbcSignupRequestRepository implements SignupRequestRepository {

    private static final String SELECT_COLUMNS =
            "id, company_id, email, name, password, attachment_filename, attachment_content_type, "
                    + "attachment_path, status, reject_reason";

    private final JdbcTemplate jdbcTemplate;

    public JdbcSignupRequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<SignupRequest> findById(Long id) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM signup_request WHERE id = ?",
                this::mapRow,
                id
        ).stream().findFirst();
    }

    @Override
    public List<SignupRequest> findAllByCompanyIdAndStatus(Long companyId, SignupStatus status) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM signup_request WHERE company_id = ? AND status = ? ORDER BY id",
                this::mapRow,
                companyId, status.name()
        );
    }

    @Override
    public boolean existsPendingByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM signup_request WHERE email = ? AND status = ?",
                Integer.class,
                email, SignupStatus.PENDING.name()
        );
        return count != null && count > 0;
    }

    @Override
    public SignupRequest save(
            Long companyId,
            String email,
            String name,
            String encodedPassword,
            String attachmentFilename,
            String attachmentContentType,
            String attachmentPath) {

        String sql = "INSERT INTO signup_request"
                + "(company_id, email, name, password, attachment_filename, attachment_content_type, attachment_path) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, companyId);
            ps.setString(2, email);
            ps.setString(3, name);
            ps.setString(4, encodedPassword);
            ps.setString(5, attachmentFilename);
            ps.setString(6, attachmentContentType);
            ps.setString(7, attachmentPath);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return new SignupRequest(
                key == null ? null : key.longValue(),
                companyId, email, name, encodedPassword,
                attachmentFilename, attachmentContentType, attachmentPath,
                SignupStatus.PENDING, null);
    }

    @Override
    public void approve(Long id) {
        jdbcTemplate.update(
                "UPDATE signup_request SET status = ?, reviewed_at = CURRENT_TIMESTAMP WHERE id = ?",
                SignupStatus.APPROVED.name(), id
        );
    }

    @Override
    public void reject(Long id, String reason) {
        jdbcTemplate.update(
                "UPDATE signup_request SET status = ?, reject_reason = ?, reviewed_at = CURRENT_TIMESTAMP WHERE id = ?",
                SignupStatus.REJECTED.name(), reason, id
        );
    }

    private SignupRequest mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new SignupRequest(
                rs.getLong("id"),
                rs.getLong("company_id"),
                rs.getString("email"),
                rs.getString("name"),
                rs.getString("password"),
                rs.getString("attachment_filename"),
                rs.getString("attachment_content_type"),
                rs.getString("attachment_path"),
                SignupStatus.valueOf(rs.getString("status")),
                rs.getString("reject_reason")
        );
    }
}
