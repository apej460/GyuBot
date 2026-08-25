package com.gyubot.user.web.dto;

import com.gyubot.user.domain.SignupRequest;

public record SignupRequestResponse(
        Long id,
        String email,
        String name,
        String attachmentFilename,
        String status,
        String rejectReason
) {
    public static SignupRequestResponse from(SignupRequest request) {
        return new SignupRequestResponse(
                request.id(),
                request.email(),
                request.name(),
                request.attachmentFilename(),
                request.status().name(),
                request.rejectReason());
    }
}
