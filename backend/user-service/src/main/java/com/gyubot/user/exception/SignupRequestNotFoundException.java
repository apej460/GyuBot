package com.gyubot.user.exception;

public class SignupRequestNotFoundException extends RuntimeException {
    public SignupRequestNotFoundException() {
        super("가입 신청을 찾을 수 없습니다.");
    }
}
