package com.gyubot.chat.security;

import com.gyubot.chat.domain.Role;

public record AuthPrincipal(Long userId, String email, Long companyId, Role role) {
}
