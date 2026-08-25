package com.gyubot.document.security;

import com.gyubot.document.domain.Role;

public record AuthPrincipal(Long userId, String email, Long companyId, Role role) {
}
