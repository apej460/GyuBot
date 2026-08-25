package com.gyubot.user.security;

import com.gyubot.user.domain.Role;

public record AuthPrincipal(Long userId, String email, Long companyId, Role role) {
}
