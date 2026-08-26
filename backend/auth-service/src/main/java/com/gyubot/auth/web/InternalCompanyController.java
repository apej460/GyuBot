package com.gyubot.auth.web;

import com.gyubot.auth.domain.Company;
import com.gyubot.auth.exception.InternalAuthException;
import com.gyubot.auth.service.CompanyService;
import com.gyubot.auth.web.dto.CompanyResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
 * user-service가 일반 가입(회사 이메일 도메인 인증) 접수 시, 입력된 이메일 도메인이 등록된
 * 회사인지 확인하고 companyId를 받아가기 위해 호출하는 내부 전용 API.
 */
@RestController
@RequestMapping("/internal/companies")
public class InternalCompanyController {

    private final CompanyService companyService;
    private final String internalToken;

    public InternalCompanyController(CompanyService companyService, @Value("${app.internal.token}") String internalToken) {
        this.companyService = companyService;
        this.internalToken = internalToken;
    }

    @GetMapping("/resolve")
    public CompanyResponse resolve(@RequestParam String email, @RequestHeader("X-Internal-Token") String token) {
        if (!internalToken.equals(token)) {
            throw new InternalAuthException();
        }
        Company company = companyService.resolveByEmail(email);
        return CompanyResponse.from(company);
    }
}
