package com.gyubot.user.service;

import com.gyubot.user.client.AuthServiceClient;
import com.gyubot.user.domain.MemberProfile;
import com.gyubot.user.domain.MemberStatus;
import com.gyubot.user.domain.Role;
import com.gyubot.user.repository.MemberProfileRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
 * 관리자 계정 관리(SCR-ADMIN-003) - 회원 관리(MemberService/일반 임직원)와는 별개로,
 * ADMIN/SUPER_ADMIN 역할을 가진 계정만 다룬다. SUPER_ADMIN은 전체 회사, 일반 ADMIN은
 * 본인 회사 소속 관리자만 본다.
 */
@Service
public class ManagerService {

    private static final List<Role> MANAGER_ROLES = List.of(Role.ADMIN, Role.SUPER_ADMIN);

    private final MemberProfileRepository memberProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthServiceClient authServiceClient;

    public ManagerService(
            MemberProfileRepository memberProfileRepository,
            PasswordEncoder passwordEncoder,
            AuthServiceClient authServiceClient) {
        this.memberProfileRepository = memberProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.authServiceClient = authServiceClient;
    }

    @Transactional(readOnly = true)
    public List<MemberProfile> list(Long companyId, boolean allCompanies) {
        return allCompanies
                ? memberProfileRepository.findByRoles(MANAGER_ROLES)
                : memberProfileRepository.findByCompanyIdAndRoles(companyId, MANAGER_ROLES);
    }

    @Transactional
    public MemberProfile create(Long companyId, String email, String rawPassword, String name, Role role) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        Long userId = authServiceClient.createUser(companyId, email, encodedPassword, name, role.name());
        memberProfileRepository.insert(userId, companyId, email, name, null, null, role, MemberStatus.ACTIVE);
        return memberProfileRepository.findById(userId).orElseThrow();
    }
}
