package com.gyubot.user.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailSignupOtpRequest(@NotBlank @Email String email) {
}
