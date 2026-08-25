package com.gyubot.auth.repository;

import com.gyubot.auth.domain.AuthUser;
import com.gyubot.auth.domain.Role;

import java.util.Optional;

public interface AuthUserRepository {
    Optional<AuthUser> findByEmail(String email);

    Optional<AuthUser> findById(Long id);

    AuthUser save(Long companyId, String email, String encodedPassword, String name, Role role);
}
