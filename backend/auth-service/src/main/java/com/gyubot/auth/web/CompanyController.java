package com.gyubot.auth.web;

import com.gyubot.auth.service.CompanyService;
import com.gyubot.auth.web.dto.CompanyResponse;
import com.gyubot.auth.web.dto.RegisterCompanyRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
 * 시스템 관리(SCR-SYS-001) - SUPER_ADMIN 전용 테넌트(회사) 등록·조회. SecurityConfig에서
 * /api/companies/**를 hasRole("SUPER_ADMIN")으로 막아둔다.
 */
@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public List<CompanyResponse> list() {
        return companyService.listAll().stream().map(CompanyResponse::from).toList();
    }

    @PostMapping
    public CompanyResponse register(@Valid @RequestBody RegisterCompanyRequest request) {
        return CompanyResponse.from(companyService.register(request.name(), request.emailDomain()));
    }
}
