package com.gyubot.auth.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequestRequest(@NotBlank @Email String email) {
}
