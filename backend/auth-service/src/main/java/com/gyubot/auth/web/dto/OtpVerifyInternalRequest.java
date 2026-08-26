package com.gyubot.auth.web.dto;

import com.gyubot.auth.otp.OtpPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OtpVerifyInternalRequest(@NotBlank @Email String email, @NotBlank String code, @NotNull OtpPurpose purpose) {
}
