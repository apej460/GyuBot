package com.gyubot.user.web.dto;

import com.gyubot.user.domain.MemberProfile;

public record MemberResponse(
        Long id, Long companyId, String email, String name, String department, String position,
        String role, String status
) {
    public static MemberResponse from(MemberProfile profile) {
        return new MemberResponse(
                profile.id(),
                profile.companyId(),
                profile.email(),
                profile.name(),
                profile.department(),
                profile.position(),
                profile.role().name(),
                profile.status().name());
    }
}
