package com.gyubot.auth.security;

import com.gyubot.auth.domain.Role;

public record AuthPrincipal(Long userId, String email, Long companyId, Role role) {
}
