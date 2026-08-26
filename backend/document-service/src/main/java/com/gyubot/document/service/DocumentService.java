package com.gyubot.document.service;

import com.gyubot.document.domain.Document;
import com.gyubot.document.event.DocumentEventPublisher;
import com.gyubot.document.exception.DocumentNotFoundException;
import com.gyubot.document.exception.InvalidDocumentFileException;
import com.gyubot.document.repository.DocumentRepository;
import com.gyubot.document.storage.S3StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.time.LocalDate;
import java.util.List;

@Service
public class DocumentService {

    // REQ-F-012: 파일당 최대 50MB, PDF·HWP만 지원
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final S3StorageService s3StorageService;
    private final DocumentEventPublisher documentEventPublisher;

    public DocumentService(
            DocumentRepository documentRepository,
            S3StorageService s3StorageService,
            DocumentEventPublisher documentEventPublisher) {
        this.documentRepository = documentRepository;
        this.s3StorageService = s3StorageService;
        this.documentEventPublisher = documentEventPublisher;
    }

    @Transactional
    public Document upload(
            Long companyId, String title, String version, String category,
            LocalDate effectiveDate, LocalDate revisionDate, MultipartFile file) {
        validate(file);

        String s3Key = S3StorageService.newKey(companyId, file.getOriginalFilename());
        s3StorageService.upload(file, s3Key);

        Document saved = documentRepository.save(
                companyId, title, file.getOriginalFilename(), file.getContentType(), file.getSize(), s3Key,
                version, category, effectiveDate, revisionDate);
        documentEventPublisher.publishUploaded(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Document> listByCompany(Long companyId) {
        return documentRepository.findAllByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public int countByCompany(Long companyId) {
        return documentRepository.countByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public Document requireById(Long id) {
        return documentRepository.findById(id).orElseThrow(DocumentNotFoundException::new);
    }

    @Transactional
    public Document updateTitle(Long id, String title) {
        requireById(id);
        documentRepository.updateTitle(id, title);
        return requireById(id);
    }

    @Transactional
    public void delete(Long id) {
        Document document = requireById(id);
        documentRepository.softDelete(id);
        s3StorageService.delete(document.s3Key());
        documentEventPublisher.publishDeleted(document);
    }

    /*
     * 다운로드는 관리자뿐 아니라 챗봇 답변의 "원문 확인"을 통해 임직원도 호출한다.
     * 다른 회사 문서를 id만으로 접근하지 못하도록 companyId를 반드시 확인한다 (REQ-F-018).
     */
    public DownloadedFile download(Long id, Long companyId) {
        Document document = requireByIdForCompany(id, companyId);
        return download(document);
    }

    /*
     * 챗봇 답변의 근거 문서 카드(버전/시행일 등 메타데이터)를 임직원도 조회할 수 있어야 해서,
     * 관리자 전용인 detail()과 별도로 companyId를 확인하는 조회를 둔다.
     */
    @Transactional(readOnly = true)
    public Document requireByIdForCompany(Long id, Long companyId) {
        Document document = requireById(id);
        if (!document.companyId().equals(companyId)) {
            throw new DocumentNotFoundException();
        }
        return document;
    }

    /*
     * search-service가 Chunking을 위해 원문을 받아가는 내부 API 전용 — 이미 X-Internal-Token으로
     * 인증된 신뢰할 수 있는 서비스 간 호출이라 companyId 확인이 필요 없다.
     */
    public DownloadedFile download(Long id) {
        return download(requireById(id));
    }

    private DownloadedFile download(Document document) {
        ResponseInputStream<GetObjectResponse> content = s3StorageService.download(document.s3Key());
        return new DownloadedFile(document, content);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentFileException("파일을 첨부해주세요.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidDocumentFileException("파일 크기는 50MB를 초과할 수 없습니다.");
        }
        String filename = file.getOriginalFilename();
        boolean validExtension = filename != null
                && (filename.toLowerCase().endsWith(".pdf") || filename.toLowerCase().endsWith(".hwp"));
        if (!validExtension) {
            throw new InvalidDocumentFileException("PDF 또는 HWP 파일만 업로드할 수 있습니다.");
        }
    }

    public record DownloadedFile(Document document, ResponseInputStream<GetObjectResponse> content) {
    }
}
