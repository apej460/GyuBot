package com.gyubot.document.web;

import com.gyubot.document.security.AuthPrincipal;
import com.gyubot.document.service.DocumentService;
import com.gyubot.document.web.dto.DocumentResponse;
import com.gyubot.document.web.dto.DocumentStatsResponse;
import com.gyubot.document.web.dto.UpdateDocumentRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /*
     * 관리자 - 문서 등록 (S3 저장 + document.uploaded 이벤트 발행)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentResponse upload(
            Authentication authentication,
            @RequestParam String title,
            @RequestParam(required = false) String version,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate revisionDate,
            @RequestParam("file") MultipartFile file) {
        Long companyId = principalOf(authentication).companyId();
        return DocumentResponse.from(
                documentService.upload(companyId, title, version, category, effectiveDate, revisionDate, file));
    }

    /*
     * 관리자 - 문서 목록 (본인 회사 소속만)
     */
    @GetMapping
    public List<DocumentResponse> list(Authentication authentication) {
        Long companyId = principalOf(authentication).companyId();
        return documentService.listByCompany(companyId).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    /*
     * 관리자 - 운영 대시보드용 문서 수 (본인 회사 소속만)
     */
    @GetMapping("/stats")
    public DocumentStatsResponse stats(Authentication authentication) {
        return new DocumentStatsResponse(documentService.countByCompany(principalOf(authentication).companyId()));
    }

    /*
     * 관리자 - 문서 상세
     */
    @GetMapping("/{id}")
    public DocumentResponse detail(@PathVariable Long id) {
        return DocumentResponse.from(documentService.requireById(id));
    }

    /*
     * 관리자 - 문서 제목 수정
     */
    @PatchMapping("/{id}")
    public DocumentResponse update(@PathVariable Long id, @Valid @RequestBody UpdateDocumentRequest request) {
        return DocumentResponse.from(documentService.updateTitle(id, request.title()));
    }

    /*
     * 관리자 - 문서 삭제 (S3 원본도 함께 삭제, document.deleted 이벤트 발행)
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        documentService.delete(id);
    }

    /*
     * 임직원·관리자 공통 - 챗봇 답변의 근거 문서 카드에 표시할 문서 메타데이터 (본인 회사 소속만)
     */
    @GetMapping("/{id}/citation")
    public DocumentResponse citation(Authentication authentication, @PathVariable Long id) {
        Long companyId = principalOf(authentication).companyId();
        return DocumentResponse.from(documentService.requireByIdForCompany(id, companyId));
    }

    /*
     * 임직원·관리자 공통 - 원본 파일 다운로드 (챗봇 답변의 "원문 확인"에서도 호출됨)
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(Authentication authentication, @PathVariable Long id) {
        Long companyId = principalOf(authentication).companyId();
        DocumentService.DownloadedFile file = documentService.download(id, companyId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.document().contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.document().originalFilename() + "\"")
                .body(new InputStreamResource(file.content()));
    }

    private AuthPrincipal principalOf(Authentication authentication) {
        return (AuthPrincipal) authentication.getPrincipal();
    }
}
