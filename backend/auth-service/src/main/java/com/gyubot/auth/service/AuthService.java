package com.gyubot.auth.service;

import com.gyubot.auth.domain.AuthUser;
import com.gyubot.auth.domain.Role;
import com.gyubot.auth.exception.DuplicateEmailException;
import com.gyubot.auth.exception.InvalidCredentialsException;
import com.gyubot.auth.repository.AuthUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthUserRepository authUserRepository, PasswordEncoder passwordEncoder) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AuthUser authenticate(String email, String rawPassword) {
        AuthUser user = authUserRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);
        if (!user.active() || !passwordEncoder.matches(rawPassword, user.password())) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    @Transactional(readOnly = true)
    public AuthUser requireByEmail(String email) {
        return authUserRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
    }

    @Transactional(readOnly = true)
    public AuthUser requireById(Long id) {
        return authUserRepository.findById(id).orElseThrow(InvalidCredentialsException::new);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        AuthUser user = requireById(userId);
        if (!passwordEncoder.matches(currentPassword, user.password())) {
            throw new InvalidCredentialsException();
        }
        authUserRepository.updatePassword(userId, passwordEncoder.encode(newPassword));
    }

    /*
     * user-service의 가입 승인 처리 전용. encodedPassword는 이미 해시된 값이므로 그대로 저장한다.
     */
    @Transactional
    public AuthUser createUser(Long companyId, String email, String encodedPassword, String name, Role role) {
        authUserRepository.findByEmail(email).ifPresent(user -> {
            throw new DuplicateEmailException();
        });
        return authUserRepository.save(companyId, email, encodedPassword, name, role);
    }
}
