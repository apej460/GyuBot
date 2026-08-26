package com.gyubot.user.service;

import com.gyubot.user.client.AuthServiceClient;
import com.gyubot.user.domain.MemberStatus;
import com.gyubot.user.domain.Role;
import com.gyubot.user.exception.DuplicateEmailException;
import com.gyubot.user.repository.MemberProfileRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * 회사 이메일 도메인으로 본인 인증(OTP)만 하면 관리자 승인 없이 바로 가입되는 일반 가입 플로우.
 * 서류(명함/재직증명서)로 관리자 검토를 거치는 예외 가입(SignupService)과는 별개다 —
 * 도메인 자체가 이미 "이 회사 소속"이라는 증거이기 때문에 승인 단계가 필요 없다.
 */
@Service
public class EmailSignupService {

    private static final String OTP_PURPOSE = "SIGNUP_EMAIL";

    private final MemberProfileRepository memberProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthServiceClient authServiceClient;

    public EmailSignupService(
            MemberProfileRepository memberProfileRepository,
            PasswordEncoder passwordEncoder,
            AuthServiceClient authServiceClient) {
        this.memberProfileRepository = memberProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.authServiceClient = authServiceClient;
    }

    /*
     * 1단계 - 이메일 도메인이 등록된 회사인지 확인하고(아니면 UnregisteredDomainException) OTP 발송.
     */
    public void requestOtp(String email) {
        if (memberProfileRepository.findByEmail(email).isPresent()) {
            throw new DuplicateEmailException();
        }
        authServiceClient.resolveCompanyByEmail(email); // 도메인 미등록이면 여기서 예외 발생
        authServiceClient.issueOtp(email, OTP_PURPOSE);
    }

    /*
     * 2단계 - OTP 검증 후 곧바로 계정 생성 (승인 대기 없음).
     */
    @Transactional
    public Long complete(
            String email, String code, String password, String name, String department, String position) {
        if (memberProfileRepository.findByEmail(email).isPresent()) {
            throw new DuplicateEmailException();
        }
        authServiceClient.verifyOtp(email, code, OTP_PURPOSE);
        AuthServiceClient.CompanyInfo company = authServiceClient.resolveCompanyByEmail(email);

        String encodedPassword = passwordEncoder.encode(password);
        Long userId = authServiceClient.createUser(company.id(), email, encodedPassword, name, "EMPLOYEE");
        memberProfileRepository.insert(
                userId, company.id(), email, name, department, position, Role.EMPLOYEE, MemberStatus.ACTIVE);
        return userId;
    }
}
