package com.gyubot.auth.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterCompanyRequest(@NotBlank String name, @NotBlank String emailDomain) {
}
