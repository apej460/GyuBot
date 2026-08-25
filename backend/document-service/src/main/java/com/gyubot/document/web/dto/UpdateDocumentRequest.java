package com.gyubot.document.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateDocumentRequest(@NotBlank String title) {
}
