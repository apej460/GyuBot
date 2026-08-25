package com.gyubot.document.domain;

public record Document(
        Long id,
        Long companyId,
        String title,
        String originalFilename,
        String contentType,
        long fileSize,
        String s3Key
) {
}
