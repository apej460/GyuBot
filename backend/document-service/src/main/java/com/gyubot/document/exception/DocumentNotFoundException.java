package com.gyubot.document.exception;

public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException() {
        super("문서를 찾을 수 없습니다.");
    }
}
