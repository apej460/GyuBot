package com.gyubot.document.web;

import com.gyubot.document.exception.InternalAuthException;
import com.gyubot.document.service.DocumentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * search-service가 document.uploaded 이벤트를 받고 원본 파일을 내려받기 위해 호출하는 내부 전용 API.
 * 최종 사용자가 직접 호출하지 않으므로 JWT가 아니라 서비스 간 공유 시크릿(X-Internal-Token)으로 보호한다.
 */
@RestController
@RequestMapping("/internal/documents")
public class InternalDocumentController {

    private final DocumentService documentService;
    private final String internalToken;

    public InternalDocumentController(DocumentService documentService, @Value("${app.internal.token}") String internalToken) {
        this.documentService = documentService;
        this.internalToken = internalToken;
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id, @RequestHeader("X-Internal-Token") String token) {
        if (!internalToken.equals(token)) {
            throw new InternalAuthException();
        }
        DocumentService.DownloadedFile file = documentService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.document().contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.document().originalFilename() + "\"")
                .body(new InputStreamResource(file.content()));
    }
}
