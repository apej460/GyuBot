package com.gyubot.auth.web;

import com.gyubot.auth.domain.AuthUser;
import com.gyubot.auth.domain.Role;
import com.gyubot.auth.exception.InvalidCredentialsException;
import com.gyubot.auth.otp.OtpPurpose;
import com.gyubot.auth.otp.OtpService;
import com.gyubot.auth.security.AuthPrincipal;
import com.gyubot.auth.security.CookieService;
import com.gyubot.auth.security.JwtProperties;
import com.gyubot.auth.security.JwtProvider;
import com.gyubot.auth.service.AuthService;
import com.gyubot.auth.token.RefreshTokenService;
import com.gyubot.auth.web.dto.AuthCheckResponse;
import com.gyubot.auth.web.dto.LoginRequest;
import com.gyubot.auth.web.dto.LoginResponse;
import com.gyubot.auth.web.dto.OtpVerifyRequest;
import com.gyubot.auth.web.dto.PasswordResetConfirmRequest;
import com.gyubot.auth.web.dto.PasswordResetRequestRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final CookieService cookieService;

    public AuthController(
            AuthService authService,
            OtpService otpService,
            RefreshTokenService refreshTokenService,
            JwtProvider jwtProvider,
            JwtProperties jwtProperties,
            CookieService cookieService) {
        this.authService = authService;
        this.otpService = otpService;
        this.refreshTokenService = refreshTokenService;
        this.jwtProvider = jwtProvider;
        this.jwtProperties = jwtProperties;
        this.cookieService = cookieService;
    }

    /*
     * 이메일/PW 로그인.
     * ADMIN 계정은 여기서 바로 토큰을 내주지 않고 OTP를 발송한 뒤 대기시킵니다 (관리자 2차 인증).
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthUser user = authService.authenticate(request.email(), request.password());

        if (user.role() == Role.ADMIN || user.role() == Role.SUPER_ADMIN) {
            otpService.issue(user.email());
            return new LoginResponse(true, "관리자 계정입니다. 이메일로 발송된 인증번호를 입력해주세요.");
        }

        issueTokens(user, response);
        return new LoginResponse(false, "로그인되었습니다.");
    }

    @PostMapping("/otp/verify")
    public LoginResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request, HttpServletResponse response) {
        otpService.verify(request.email(), request.code());
        AuthUser user = authService.requireByEmail(request.email());
        issueTokens(user, response);
        return new LoginResponse(false, "인증되었습니다.");
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieService.read(request, CookieService.REFRESH_TOKEN)
                .orElseThrow(InvalidCredentialsException::new);

        Long userId = refreshTokenService.resolve(refreshToken);
        refreshTokenService.revoke(refreshToken);

        AuthUser user = authService.requireById(userId);
        issueTokens(user, response);
        return new LoginResponse(false, "토큰이 갱신되었습니다.");
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        cookieService.read(request, CookieService.REFRESH_TOKEN).ifPresent(refreshTokenService::revoke);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.delete(CookieService.ACCESS_TOKEN).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.delete(CookieService.REFRESH_TOKEN).toString());
    }

    /*
     * 비밀번호 재설정 1단계 - 이메일로 OTP 발송. 계정 소유자만 재설정할 수 있게 소유권을 증명시킨다.
     */
    @PostMapping("/password-reset/request")
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequestRequest request) {
        authService.requireByEmail(request.email());
        otpService.issue(request.email(), OtpPurpose.PASSWORD_RESET);
    }

    /*
     * 비밀번호 재설정 2단계 - OTP 검증 후 곧바로 새 비밀번호로 교체.
     */
    @PostMapping("/password-reset/confirm")
    public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        otpService.verify(request.email(), request.code(), OtpPurpose.PASSWORD_RESET);
        authService.resetPassword(request.email(), request.newPassword());
    }

    @GetMapping("/check")
    public AuthCheckResponse check(Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        return new AuthCheckResponse(principal.userId(), principal.email(), principal.companyId(), principal.role().name());
    }

    private void issueTokens(AuthUser user, HttpServletResponse response) {
        String accessToken = jwtProvider.createAccessToken(user);
        String refreshToken = refreshTokenService.issue(user.id());

        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieService.issue(CookieService.ACCESS_TOKEN, accessToken, jwtProperties.accessTokenTtl()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieService.issue(CookieService.REFRESH_TOKEN, refreshToken, jwtProperties.refreshTokenTtl()).toString());
    }
}
