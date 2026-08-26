package com.gyubot.user.exception;

public class InsufficientRoleException extends RuntimeException {
    public InsufficientRoleException() {
        super("최고관리자만 최고관리자 계정을 만들 수 있습니다.");
    }
}
