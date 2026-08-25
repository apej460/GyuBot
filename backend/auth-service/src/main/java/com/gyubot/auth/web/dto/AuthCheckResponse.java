package com.gyubot.auth.web.dto;

public record AuthCheckResponse(Long userId, String email, Long companyId, String role) {
}
