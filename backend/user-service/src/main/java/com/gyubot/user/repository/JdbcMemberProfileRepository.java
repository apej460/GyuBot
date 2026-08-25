package com.gyubot.user.repository;

import com.gyubot.user.domain.MemberProfile;
import com.gyubot.user.domain.MemberStatus;
import com.gyubot.user.domain.Role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcMemberProfileRepository implements MemberProfileRepository {

    private static final String SELECT_COLUMNS =
            "id, company_id, email, name, role, status";

    private final JdbcTemplate jdbcTemplate;

    public JdbcMemberProfileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<MemberProfile> findById(Long id) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM member_profile WHERE id = ?",
                this::mapRow,
                id
        ).stream().findFirst();
    }

    @Override
    public List<MemberProfile> findAllByCompanyId(Long companyId) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM member_profile WHERE company_id = ? ORDER BY id",
                this::mapRow,
                companyId
        );
    }

    @Override
    public void updateStatus(Long id, MemberStatus status) {
        jdbcTemplate.update(
                "UPDATE member_profile SET status = ? WHERE id = ?",
                status.name(), id
        );
    }

    private MemberProfile mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new MemberProfile(
                rs.getLong("id"),
                rs.getLong("company_id"),
                rs.getString("email"),
                rs.getString("name"),
                Role.valueOf(rs.getString("role")),
                MemberStatus.valueOf(rs.getString("status"))
        );
    }
}
