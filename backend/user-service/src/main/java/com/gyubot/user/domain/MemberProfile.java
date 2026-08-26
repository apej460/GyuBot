package com.gyubot.user.domain;

public record MemberProfile(
        Long id,
        Long companyId,
        String email,
        String name,
        String department,
        String position,
        Role role,
        MemberStatus status
) {
}
