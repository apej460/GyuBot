package com.gyubot.document.web.dto;

import com.gyubot.document.domain.Document;

public record DocumentResponse(
        Long id,
        Long companyId,
        String title,
        String originalFilename,
        String contentType,
        long fileSize
) {
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.id(),
                document.companyId(),
                document.title(),
                document.originalFilename(),
                document.contentType(),
                document.fileSize());
    }
}
