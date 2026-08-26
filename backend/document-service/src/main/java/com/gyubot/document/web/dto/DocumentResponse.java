package com.gyubot.document.web.dto;

import com.gyubot.document.domain.Document;

import java.time.LocalDate;

public record DocumentResponse(
        Long id,
        Long companyId,
        String title,
        String originalFilename,
        String contentType,
        long fileSize,
        String version,
        String category,
        LocalDate effectiveDate,
        LocalDate revisionDate,
        String status
) {
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.id(),
                document.companyId(),
                document.title(),
                document.originalFilename(),
                document.contentType(),
                document.fileSize(),
                document.version(),
                document.category(),
                document.effectiveDate(),
                document.revisionDate(),
                statusOf(document));
    }

    // 시행일이 아직 안 된 문서는 "시행 예정", 지났거나 없으면 "사용 중"으로 계산해서 보여준다.
    private static String statusOf(Document document) {
        if (document.effectiveDate() != null && document.effectiveDate().isAfter(LocalDate.now())) {
            return "UPCOMING";
        }
        return "ACTIVE";
    }
}
