package com.gyubot.user.exception;

public class OtpVerificationFailedException extends RuntimeException {
    public OtpVerificationFailedException() {
        super("인증번호가 올바르지 않거나 만료되었습니다.");
    }
}
