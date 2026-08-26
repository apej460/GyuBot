package com.gyubot.user.exception;

public class UnregisteredDomainException extends RuntimeException {
    public UnregisteredDomainException() {
        super("등록되지 않은 회사 이메일입니다. 예외 가입을 이용해주세요.");
    }
}
