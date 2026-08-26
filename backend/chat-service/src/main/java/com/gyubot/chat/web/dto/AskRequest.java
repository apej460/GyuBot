package com.gyubot.chat.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AskRequest(Long sessionId, @NotBlank String question) {
}
