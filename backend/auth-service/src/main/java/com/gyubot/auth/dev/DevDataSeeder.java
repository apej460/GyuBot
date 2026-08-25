package com.gyubot.auth.dev;

import com.gyubot.auth.domain.Role;
import com.gyubot.auth.repository.AuthUserRepository;
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

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(AuthUserRepository authUserRepository, PasswordEncoder passwordEncoder) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seed("employee@gyubot.local", "테스트 임직원", Role.EMPLOYEE);
        seed("admin@gyubot.local", "테스트 관리자", Role.ADMIN);
    }

    private void seed(String email, String name, Role role) {
        if (authUserRepository.findByEmail(email).isPresent()) {
            return;
        }
        authUserRepository.save(DEV_COMPANY_ID, email, passwordEncoder.encode(DEV_PASSWORD), name, role);
        log.info("[dev-seed] {} 계정 생성: {} / {}", role, email, DEV_PASSWORD);
    }
}
