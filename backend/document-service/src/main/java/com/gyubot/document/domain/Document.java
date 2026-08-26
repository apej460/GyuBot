package com.gyubot.document.domain;

import java.time.LocalDate;

public record Document(
        Long id,
        Long companyId,
        String title,
        String originalFilename,
        String contentType,
        long fileSize,
        String s3Key,
        String version,
        String category,
        LocalDate effectiveDate,
        LocalDate revisionDate
) {
}
