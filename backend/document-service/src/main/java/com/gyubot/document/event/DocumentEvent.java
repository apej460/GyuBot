package com.gyubot.document.event;

/*
 * document.uploaded / document.deleted 토픽으로 발행되는 이벤트.
 * search-service가 이를 구독해 Chunking·색인(업로드) 또는 인덱스 제거(삭제)를 비동기로 수행한다.
 */
public record DocumentEvent(
        Long documentId,
        Long companyId,
        String s3Key,
        String originalFilename,
        String contentType
) {
}
