package com.gyubot.user.domain;

public record SignupRequest(
        Long id,
        Long companyId,
        String email,
        String name,
        String encodedPassword,
        String attachmentFilename,
        String attachmentContentType,
        String attachmentPath,
        SignupStatus status,
        String rejectReason
) {
}
