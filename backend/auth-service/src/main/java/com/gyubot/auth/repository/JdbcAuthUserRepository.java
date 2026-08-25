package com.gyubot.auth.repository;

import com.gyubot.auth.domain.AuthUser;
import com.gyubot.auth.domain.Role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class JdbcAuthUserRepository implements AuthUserRepository {

    private static final String SELECT_COLUMNS =
            "id, company_id, email, password, name, role, active";

    private final JdbcTemplate jdbcTemplate;

    public JdbcAuthUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<AuthUser> findByEmail(String email) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM auth_user WHERE email = ?",
                this::mapRow,
                email
        ).stream().findFirst();
    }

    @Override
    public Optional<AuthUser> findById(Long id) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM auth_user WHERE id = ?",
                this::mapRow,
                id
        ).stream().findFirst();
    }

    @Override
    public AuthUser save(Long companyId, String email, String encodedPassword, String name, Role role) {
        String sql = "INSERT INTO auth_user(company_id, email, password, name, role) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, companyId);
            ps.setString(2, email);
            ps.setString(3, encodedPassword);
            ps.setString(4, name);
            ps.setString(5, role.name());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return new AuthUser(key == null ? null : key.longValue(), companyId, email, encodedPassword, name, role, true);
    }

    @Override
    public void updatePassword(Long id, String encodedPassword) {
        jdbcTemplate.update(
                "UPDATE auth_user SET password = ? WHERE id = ?",
                encodedPassword, id
        );
    }

    private AuthUser mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new AuthUser(
                rs.getLong("id"),
                rs.getLong("company_id"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("name"),
                Role.valueOf(rs.getString("role")),
                rs.getBoolean("active")
        );
    }
}
