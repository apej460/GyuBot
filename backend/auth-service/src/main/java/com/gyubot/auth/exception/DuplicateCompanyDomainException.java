package com.gyubot.auth.exception;

public class DuplicateCompanyDomainException extends RuntimeException {
    public DuplicateCompanyDomainException() {
        super("이미 등록된 이메일 도메인입니다.");
    }
}
