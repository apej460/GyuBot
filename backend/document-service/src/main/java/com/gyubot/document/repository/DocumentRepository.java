package com.gyubot.document.repository;

import com.gyubot.document.domain.Document;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository {
    Optional<Document> findById(Long id);

    List<Document> findAllByCompanyId(Long companyId);

    int countByCompanyId(Long companyId);

    Document save(
            Long companyId, String title, String originalFilename, String contentType, long fileSize, String s3Key,
            String version, String category, LocalDate effectiveDate, LocalDate revisionDate);

    void updateTitle(Long id, String title);

    void softDelete(Long id);
}
