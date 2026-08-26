package com.gyubot.user.web;

import com.gyubot.user.domain.Role;
import com.gyubot.user.exception.InsufficientRoleException;
import com.gyubot.user.security.AuthPrincipal;
import com.gyubot.user.service.ManagerService;
import com.gyubot.user.web.dto.CreateManagerRequest;
import com.gyubot.user.web.dto.MemberResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
 * 관리자 계정 관리(SCR-ADMIN-003) - 일반 회원 관리(MemberController)와 별개로 ADMIN/SUPER_ADMIN
 * 계정만 다룬다. SecurityConfig의 "/api/users/**" -> hasRole("ADMIN") 규칙을 그대로 물려받는다.
 */
@RestController
@RequestMapping("/api/users/managers")
public class ManagerController {

    private final ManagerService managerService;

    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @GetMapping
    public List<MemberResponse> list(Authentication authentication) {
        AuthPrincipal principal = principalOf(authentication);
        boolean allCompanies = principal.role() == Role.SUPER_ADMIN;
        return managerService.list(principal.companyId(), allCompanies).stream()
                .map(MemberResponse::from)
                .toList();
    }

    /*
     * SUPER_ADMIN 계정 생성은 SUPER_ADMIN만 할 수 있다 — 일반 ADMIN이 자신과 동급 이상의
     * 계정을 만들어버리는 권한 상승을 막는다.
     */
    @PostMapping
    public MemberResponse create(Authentication authentication, @Valid @RequestBody CreateManagerRequest request) {
        AuthPrincipal principal = principalOf(authentication);
        Role role = Role.valueOf(request.role());
        if (role == Role.SUPER_ADMIN && principal.role() != Role.SUPER_ADMIN) {
            throw new InsufficientRoleException();
        }
        return MemberResponse.from(
                managerService.create(principal.companyId(), request.email(), request.password(), request.name(), role));
    }

    private AuthPrincipal principalOf(Authentication authentication) {
        return (AuthPrincipal) authentication.getPrincipal();
    }
}
