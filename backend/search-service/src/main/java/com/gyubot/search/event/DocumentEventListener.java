package com.gyubot.search.event;

import com.gyubot.search.exception.UnsupportedFileFormatException;
import com.gyubot.search.index.DocumentChunkIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DocumentEventListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentEventListener.class);

    private final DocumentChunkIndexer documentChunkIndexer;

    public DocumentEventListener(DocumentChunkIndexer documentChunkIndexer) {
        this.documentChunkIndexer = documentChunkIndexer;
    }

    @KafkaListener(topics = "${app.kafka.document-uploaded-topic}")
    public void onDocumentUploaded(DocumentEvent event) {
        try {
            documentChunkIndexer.index(event);
        } catch (UnsupportedFileFormatException e) {
            // HWP 등 아직 지원하지 않는 형식은 색인만 건너뛴다 — 문서 자체는 document-service에 정상 저장돼 있다.
            log.warn("문서 {} 색인을 건너뜁니다: {}", event.documentId(), e.getMessage());
        }
    }

    @KafkaListener(topics = "${app.kafka.document-deleted-topic}")
    public void onDocumentDeleted(DocumentEvent event) {
        documentChunkIndexer.deleteByDocumentId(event.documentId());
        log.info("문서 {} 청크 삭제 완료", event.documentId());
    }
}
