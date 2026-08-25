package com.gyubot.user.exception;

public class PasswordChangeException extends RuntimeException {
    public PasswordChangeException() {
        super("현재 비밀번호가 올바르지 않습니다.");
    }
}
