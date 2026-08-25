package com.gyubot.user.repository;

import com.gyubot.user.domain.MemberProfile;
import com.gyubot.user.domain.MemberStatus;

import java.util.List;
import java.util.Optional;

public interface MemberProfileRepository {
    Optional<MemberProfile> findById(Long id);

    List<MemberProfile> findAllByCompanyId(Long companyId);

    void updateStatus(Long id, MemberStatus status);
}
