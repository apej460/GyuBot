package com.gyubot.document.repository;

import com.gyubot.document.domain.Document;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository {
    Optional<Document> findById(Long id);

    List<Document> findAllByCompanyId(Long companyId);

    Document save(Long companyId, String title, String originalFilename, String contentType, long fileSize, String s3Key);

    void updateTitle(Long id, String title);

    void softDelete(Long id);
}
