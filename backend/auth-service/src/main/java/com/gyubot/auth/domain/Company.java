package com.gyubot.auth.domain;

public record Company(
        Long id,
        String name,
        String emailDomain,
        String tenantCode,
        CompanyStatus status
) {
}
