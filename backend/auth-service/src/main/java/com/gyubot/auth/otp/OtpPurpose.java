package com.gyubot.auth.otp;

public enum OtpPurpose {
    ADMIN_LOGIN("[GyuBot] 관리자 로그인 인증번호"),
    PASSWORD_RESET("[GyuBot] 비밀번호 재설정 인증번호"),
    SIGNUP_EMAIL("[GyuBot] 회원가입 이메일 인증번호");

    private final String subject;

    OtpPurpose(String subject) {
        this.subject = subject;
    }

    public String subject() {
        return subject;
    }
}
