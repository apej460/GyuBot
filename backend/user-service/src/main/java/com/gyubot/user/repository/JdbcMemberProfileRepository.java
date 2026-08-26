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
            "id, company_id, email, name, department, position, role, status";

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
    public Optional<MemberProfile> findByEmail(String email) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM member_profile WHERE email = ?",
                this::mapRow,
                email
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
    public List<MemberProfile> findByCompanyIdAndRoles(Long companyId, List<Role> roles) {
        String placeholders = String.join(",", roles.stream().map(r -> "?").toList());
        Object[] args = concat(companyId, roles);
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM member_profile WHERE company_id = ? AND role IN (" + placeholders + ") ORDER BY id",
                this::mapRow,
                args
        );
    }

    @Override
    public List<MemberProfile> findByRoles(List<Role> roles) {
        String placeholders = String.join(",", roles.stream().map(r -> "?").toList());
        Object[] args = roles.stream().map(Role::name).toArray();
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM member_profile WHERE role IN (" + placeholders + ") ORDER BY company_id, id",
                this::mapRow,
                args
        );
    }

    @Override
    public void updateStatus(Long id, MemberStatus status) {
        jdbcTemplate.update(
                "UPDATE member_profile SET status = ? WHERE id = ?",
                status.name(), id
        );
    }

    @Override
    public void insert(
            Long id, Long companyId, String email, String name, String department, String position,
            Role role, MemberStatus status) {
        jdbcTemplate.update(
                "INSERT INTO member_profile(id, company_id, email, name, department, position, role, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id, companyId, email, name, department, position, role.name(), status.name()
        );
    }

    private Object[] concat(Long companyId, List<Role> roles) {
        Object[] args = new Object[roles.size() + 1];
        args[0] = companyId;
        for (int i = 0; i < roles.size(); i++) {
            args[i + 1] = roles.get(i).name();
        }
        return args;
    }

    private MemberProfile mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new MemberProfile(
                rs.getLong("id"),
                rs.getLong("company_id"),
                rs.getString("email"),
                rs.getString("name"),
                rs.getString("department"),
                rs.getString("position"),
                Role.valueOf(rs.getString("role")),
                MemberStatus.valueOf(rs.getString("status"))
        );
    }
}
