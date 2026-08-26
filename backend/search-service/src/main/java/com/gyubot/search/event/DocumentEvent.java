package com.gyubot.search.event;

/*
 * document-service가 document.uploaded / document.deleted 토픽으로 발행하는 이벤트.
 * document-service의 com.gyubot.document.event.DocumentEvent와 필드가 정확히 일치해야 한다
 * (JSON 역직렬화 대상 — Kafka 메시지에 타입 헤더는 무시하고 이 타입으로 바로 매핑한다).
 */
public record DocumentEvent(
        Long documentId,
        Long companyId,
        String s3Key,
        String originalFilename,
        String contentType
) {
}
