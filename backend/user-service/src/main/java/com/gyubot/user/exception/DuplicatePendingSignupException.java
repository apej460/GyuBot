package com.gyubot.user.exception;

public class DuplicatePendingSignupException extends RuntimeException {
    public DuplicatePendingSignupException() {
        super("이미 처리 대기 중인 가입 신청이 있습니다.");
    }
}
