package com.gyubot.auth.dev;

import com.gyubot.auth.domain.Role;
import com.gyubot.auth.repository.AuthUserRepository;
import com.gyubot.auth.repository.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/*
 * 로컬 개발용 시드 계정 생성기.
 * 실제 가입은 user-service의 승인 플로우를 거치지만, 아직 없으므로
 * auth-service 단독으로 로그인/OTP 흐름을 테스트할 수 있도록 계정을 미리 만들어 둡니다.
 */
@Component
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);
    private static final String DEV_PASSWORD = "Passw0rd!";
    private static final Long DEV_COMPANY_ID = 1L;
    private static final String DEV_DOMAIN = "gyubot.local";

    private final AuthUserRepository authUserRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(
            AuthUserRepository authUserRepository, CompanyRepository companyRepository, PasswordEncoder passwordEncoder) {
        this.authUserRepository = authUserRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedCompany();
        seed("employee@gyubot.local", "테스트 임직원", Role.EMPLOYEE);
        seed("admin@gyubot.local", "테스트 관리자", Role.ADMIN);
        seed("superadmin@gyubot.local", "테스트 최고관리자", Role.SUPER_ADMIN);
    }

    private void seedCompany() {
        if (companyRepository.findById(DEV_COMPANY_ID).isPresent()) {
            return;
        }
        companyRepository.save("GyuBot 데모 회사", DEV_DOMAIN, "TEN-100001");
        log.info("[dev-seed] 회사 등록: GyuBot 데모 회사 ({})", DEV_DOMAIN);
    }

    private void seed(String email, String name, Role role) {
        if (authUserRepository.findByEmail(email).isPresent()) {
            return;
        }
        authUserRepository.save(DEV_COMPANY_ID, email, passwordEncoder.encode(DEV_PASSWORD), name, role);
        log.info("[dev-seed] {} 계정 생성: {} / {}", role, email, DEV_PASSWORD);
    }
}
