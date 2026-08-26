package com.gyubot.user.dev;

import com.gyubot.user.domain.MemberStatus;
import com.gyubot.user.domain.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/*
 * auth-service의 DevDataSeeder가 만드는 테스트 계정(employee@gyubot.local id=1,
 * admin@gyubot.local id=2)과 id를 맞춰서 프로필을 시드한다.
 *
 * 실제로는 가입 승인 시 auth-service에 계정이 생기면서 이 서비스에도 프로필이
 * 함께 생겨야 하는데, 그 연동(내부 API 또는 이벤트)은 아직 없다. 로컬 개발용으로
 * 여기서도 동일한 계정을 직접 시드해 둔다.
 */
@Component
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final JdbcTemplate jdbcTemplate;

    public DevDataSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        seed(1L, 1L, "employee@gyubot.local", "테스트 임직원", "경영지원팀", "대리", Role.EMPLOYEE);
        seed(2L, 1L, "admin@gyubot.local", "테스트 관리자", "IT팀", "팀장", Role.ADMIN);
        seed(3L, 1L, "superadmin@gyubot.local", "테스트 최고관리자", "IT팀", "부장", Role.SUPER_ADMIN);
    }

    private void seed(Long id, Long companyId, String email, String name, String department, String position, Role role) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM member_profile WHERE id = ?", Integer.class, id);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO member_profile(id, company_id, email, name, department, position, role, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id, companyId, email, name, department, position, role.name(), MemberStatus.ACTIVE.name());
        log.info("[dev-seed] {} 프로필 생성: {} ({})", role, email, id);
    }
}
