package com.gyubot.auth.service;

import com.gyubot.auth.domain.Company;
import com.gyubot.auth.exception.DuplicateCompanyDomainException;
import com.gyubot.auth.exception.UnregisteredDomainException;
import com.gyubot.auth.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public List<Company> listAll() {
        return companyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Company resolveByEmail(String email) {
        String domain = domainOf(email);
        return companyRepository.findByEmailDomain(domain).orElseThrow(UnregisteredDomainException::new);
    }

    @Transactional
    public Company register(String name, String emailDomain) {
        companyRepository.findByEmailDomain(emailDomain).ifPresent(c -> {
            throw new DuplicateCompanyDomainException();
        });
        String tenantCode = "TEN-" + ThreadLocalRandom.current().nextInt(100000, 999999);
        return companyRepository.save(name, emailDomain, tenantCode);
    }

    private String domainOf(String email) {
        int at = email.indexOf('@');
        return at < 0 ? email : email.substring(at + 1);
    }
}
