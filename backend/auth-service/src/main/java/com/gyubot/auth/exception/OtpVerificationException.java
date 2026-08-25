package com.gyubot.auth.exception;

public class OtpVerificationException extends RuntimeException {
    public OtpVerificationException() {
        super("인증번호가 올바르지 않거나 만료되었습니다.");
    }
}
