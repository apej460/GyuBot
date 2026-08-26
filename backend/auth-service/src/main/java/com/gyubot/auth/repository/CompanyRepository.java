package com.gyubot.auth.repository;

import com.gyubot.auth.domain.Company;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository {
    List<Company> findAll();

    Optional<Company> findById(Long id);

    Optional<Company> findByEmailDomain(String emailDomain);

    Company save(String name, String emailDomain, String tenantCode);
}
