package com.gyubot.auth.web.dto;

import com.gyubot.auth.otp.OtpPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OtpIssueRequest(@NotBlank @Email String email, @NotNull OtpPurpose purpose) {
}
