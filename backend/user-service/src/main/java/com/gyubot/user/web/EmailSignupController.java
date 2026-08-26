package com.gyubot.user.web;

import com.gyubot.user.client.AuthServiceClient;
import com.gyubot.user.service.EmailSignupService;
import com.gyubot.user.web.dto.EmailSignupCompleteRequest;
import com.gyubot.user.web.dto.EmailSignupOtpRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
 * 회사 이메일 도메인 인증만으로 가입하는 일반 가입 플로우 (예외 가입과 달리 관리자 승인이 없음).
 * 로그인 없이 누구나 호출한다.
 */
@RestController
@RequestMapping("/api/users/signup/email")
public class EmailSignupController {

    private final EmailSignupService emailSignupService;
    private final AuthServiceClient authServiceClient;

    public EmailSignupController(EmailSignupService emailSignupService, AuthServiceClient authServiceClient) {
        this.emailSignupService = emailSignupService;
        this.authServiceClient = authServiceClient;
    }

    /*
     * 이메일 도메인이 등록된 회사인지 미리 확인해 화면에 회사명을 보여준다 (읽기 전용 필드).
     */
    @GetMapping("/company")
    public AuthServiceClient.CompanyInfo resolveCompany(@RequestParam String email) {
        return authServiceClient.resolveCompanyByEmail(email);
    }

    @PostMapping("/otp")
    public void requestOtp(@Valid @RequestBody EmailSignupOtpRequest request) {
        emailSignupService.requestOtp(request.email());
    }

    @PostMapping("/complete")
    public void complete(@Valid @RequestBody EmailSignupCompleteRequest request) {
        emailSignupService.complete(
                request.email(), request.code(), request.password(), request.name(),
                request.department(), request.position());
    }
}
