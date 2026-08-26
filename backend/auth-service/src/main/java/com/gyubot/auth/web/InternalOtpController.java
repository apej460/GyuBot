package com.gyubot.auth.web;

import com.gyubot.auth.exception.InternalAuthException;
import com.gyubot.auth.otp.OtpService;
import com.gyubot.auth.web.dto.OtpIssueRequest;
import com.gyubot.auth.web.dto.OtpVerifyInternalRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * user-service의 회사 이메일 도메인 기반 일반 가입(비밀번호 재설정과 같은 OTP 인프라 재사용)이
 * 호출하는 내부 전용 API. auth-service가 이미 갖춘 Redis+메일 OTP 발송을 다른 서비스가
 * 중복 구현하지 않도록 여기서 대신 처리해준다.
 */
@RestController
@RequestMapping("/internal/otp")
public class InternalOtpController {

    private final OtpService otpService;
    private final String internalToken;

    public InternalOtpController(OtpService otpService, @Value("${app.internal.token}") String internalToken) {
        this.otpService = otpService;
        this.internalToken = internalToken;
    }

    @PostMapping("/issue")
    public void issue(@Valid @RequestBody OtpIssueRequest request, @RequestHeader("X-Internal-Token") String token) {
        requireInternal(token);
        otpService.issue(request.email(), request.purpose());
    }

    @PostMapping("/verify")
    public void verify(@Valid @RequestBody OtpVerifyInternalRequest request, @RequestHeader("X-Internal-Token") String token) {
        requireInternal(token);
        otpService.verify(request.email(), request.code(), request.purpose());
    }

    private void requireInternal(String token) {
        if (!internalToken.equals(token)) {
            throw new InternalAuthException();
        }
    }
}
