package com.gyubot.user.repository;

import com.gyubot.user.domain.MemberProfile;
import com.gyubot.user.domain.MemberStatus;
import com.gyubot.user.domain.Role;

import java.util.List;
import java.util.Optional;

public interface MemberProfileRepository {
    Optional<MemberProfile> findById(Long id);

    Optional<MemberProfile> findByEmail(String email);

    List<MemberProfile> findAllByCompanyId(Long companyId);

    void updateStatus(Long id, MemberStatus status);

    void insert(Long id, Long companyId, String email, String name, Role role, MemberStatus status);
}
