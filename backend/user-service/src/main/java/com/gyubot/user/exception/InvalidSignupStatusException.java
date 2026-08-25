package com.gyubot.user.exception;

public class InvalidSignupStatusException extends RuntimeException {
    public InvalidSignupStatusException() {
        super("이미 처리된 가입 신청입니다.");
    }
}
