package com.gyubot.auth.domain;

public record AuthUser(
        Long id,
        Long companyId,
        String email,
        String password,
        String name,
        Role role,
        boolean active
) {
}
