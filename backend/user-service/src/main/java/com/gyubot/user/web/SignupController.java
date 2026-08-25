package com.gyubot.user.web;

import com.gyubot.user.domain.SignupRequest;
import com.gyubot.user.security.AuthPrincipal;
import com.gyubot.user.service.SignupService;
import com.gyubot.user.web.dto.RejectRequest;
import com.gyubot.user.web.dto.SignupRequestResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/users/signup-requests")
public class SignupController {

    private final SignupService signupService;

    public SignupController(SignupService signupService) {
        this.signupService = signupService;
    }

    /*
     * 회사 이메일이 없는 사용자의 예외 가입 신청. 로그인 없이 누구나 호출할 수 있다.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SignupRequestResponse submit(
            @RequestParam String email,
            @RequestParam String name,
            @RequestParam String password,
            @RequestParam("attachment") MultipartFile attachment) {
        return SignupRequestResponse.from(signupService.submit(email, name, password, attachment));
    }

    /*
     * 관리자 - 대기 중인 가입 신청 목록 (본인 회사 소속만)
     */
    @GetMapping
    public List<SignupRequestResponse> list(Authentication authentication) {
        Long companyId = principalOf(authentication).companyId();
        return signupService.listPending(companyId).stream()
                .map(SignupRequestResponse::from)
                .toList();
    }

    /*
     * 관리자 - 가입 신청 상세
     */
    @GetMapping("/{id}")
    public SignupRequestResponse detail(@PathVariable Long id) {
        return SignupRequestResponse.from(signupService.requireById(id));
    }

    /*
     * 관리자 - 첨부파일(명함·재직증명서) 열람
     */
    @GetMapping("/{id}/attachment")
    public ResponseEntity<Resource> attachment(@PathVariable Long id) {
        SignupRequest request = signupService.requireById(id);
        Resource resource = signupService.loadAttachment(request);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(request.attachmentContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + request.attachmentFilename() + "\"")
                .body(resource);
    }

    /*
     * 관리자 - 승인 (auth-service에 계정 생성 위임)
     */
    @PostMapping("/{id}/approve")
    public void approve(@PathVariable Long id) {
        signupService.approve(id);
    }

    /*
     * 관리자 - 반려 (사유를 이메일로 안내)
     */
    @PostMapping("/{id}/reject")
    public void reject(@PathVariable Long id, @Valid @RequestBody RejectRequest request) {
        signupService.reject(id, request.reason());
    }

    private AuthPrincipal principalOf(Authentication authentication) {
        return (AuthPrincipal) authentication.getPrincipal();
    }
}
