package com.gyubot.auth.web.dto;

import com.gyubot.auth.domain.Company;

public record CompanyResponse(Long id, String name, String emailDomain, String tenantCode, String status) {
    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.id(), company.name(), company.emailDomain(), company.tenantCode(), company.status().name());
    }
}
