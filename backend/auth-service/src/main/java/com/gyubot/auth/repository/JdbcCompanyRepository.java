package com.gyubot.auth.repository;

import com.gyubot.auth.domain.Company;
import com.gyubot.auth.domain.CompanyStatus;
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
public class JdbcCompanyRepository implements CompanyRepository {

    private static final String SELECT_COLUMNS = "id, name, email_domain, tenant_code, status";

    private final JdbcTemplate jdbcTemplate;

    public JdbcCompanyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Company> findAll() {
        return jdbcTemplate.query("SELECT " + SELECT_COLUMNS + " FROM company ORDER BY id", this::mapRow);
    }

    @Override
    public Optional<Company> findById(Long id) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM company WHERE id = ?", this::mapRow, id
        ).stream().findFirst();
    }

    @Override
    public Optional<Company> findByEmailDomain(String emailDomain) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM company WHERE email_domain = ?", this::mapRow, emailDomain
        ).stream().findFirst();
    }

    @Override
    public Company save(String name, String emailDomain, String tenantCode) {
        String sql = "INSERT INTO company(name, email_domain, tenant_code) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, emailDomain);
            ps.setString(3, tenantCode);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return new Company(key == null ? null : key.longValue(), name, emailDomain, tenantCode, CompanyStatus.ACTIVE);
    }

    private Company mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Company(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("email_domain"),
                rs.getString("tenant_code"),
                CompanyStatus.valueOf(rs.getString("status")));
    }
}
