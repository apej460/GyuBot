package com.gyubot.user.domain;

public record MemberProfile(
        Long id,
        Long companyId,
        String email,
        String name,
        Role role,
        MemberStatus status
) {
}
