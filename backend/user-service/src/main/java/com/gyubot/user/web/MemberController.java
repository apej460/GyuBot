package com.gyubot.user.web;

import com.gyubot.user.client.AuthServiceClient;
import com.gyubot.user.security.AuthPrincipal;
import com.gyubot.user.service.MemberService;
import com.gyubot.user.web.dto.ChangePasswordRequest;
import com.gyubot.user.web.dto.MemberResponse;
import com.gyubot.user.web.dto.UpdateStatusRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class MemberController {

    private final MemberService memberService;
    private final AuthServiceClient authServiceClient;

    public MemberController(MemberService memberService, AuthServiceClient authServiceClient) {
        this.memberService = memberService;
        this.authServiceClient = authServiceClient;
    }

    /*
     * 마이페이지 - 내 정보 (임직원/관리자 공통)
     */
    @GetMapping("/me")
    public MemberResponse me(Authentication authentication) {
        return MemberResponse.from(memberService.requireById(principalOf(authentication).userId()));
    }

    /*
     * 마이페이지 - 비밀번호 변경.
     * 실제 로그인 자격정보는 auth-service가 갖고 있으므로, 여기서는 본인 확인(JWT)만 하고
     * 실제 변경은 auth-service의 내부 API를 Eureka 서비스 디스커버리로 호출해 위임한다.
     */
    @PatchMapping("/me/password")
    public void changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        AuthPrincipal principal = principalOf(authentication);
        authServiceClient.changePassword(principal.userId(), request.currentPassword(), request.newPassword());
    }

    /*
     * 관리자 - 회원 목록 (본인 회사 소속만, 테넌트 격리)
     */
    @GetMapping
    public List<MemberResponse> list(Authentication authentication) {
        return memberService.findAllInCompany(principalOf(authentication).companyId()).stream()
                .map(MemberResponse::from)
                .toList();
    }

    /*
     * 관리자 - 회원 상세
     */
    @GetMapping("/{id}")
    public MemberResponse detail(@PathVariable Long id) {
        return MemberResponse.from(memberService.requireById(id));
    }

    /*
     * 관리자 - 회원 상태 변경 (활성/정지)
     */
    @PatchMapping("/{id}/status")
    public MemberResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        memberService.updateStatus(id, request.status());
        return MemberResponse.from(memberService.requireById(id));
    }

    private AuthPrincipal principalOf(Authentication authentication) {
        return (AuthPrincipal) authentication.getPrincipal();
    }
}
