package com.gyubot.document.exception;

public class InternalAuthException extends RuntimeException {
    public InternalAuthException() {
        super("내부 서비스 인증에 실패했습니다.");
    }
}
