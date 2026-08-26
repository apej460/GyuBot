package com.gyubot.user.service;

import com.gyubot.user.domain.MemberProfile;
import com.gyubot.user.domain.MemberStatus;
import com.gyubot.user.exception.MemberNotFoundException;
import com.gyubot.user.repository.MemberProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MemberService {

    private final MemberProfileRepository memberProfileRepository;

    public MemberService(MemberProfileRepository memberProfileRepository) {
        this.memberProfileRepository = memberProfileRepository;
    }

    @Transactional(readOnly = true)
    public MemberProfile requireById(Long id) {
        return memberProfileRepository.findById(id).orElseThrow(MemberNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<MemberProfile> findAllInCompany(Long companyId) {
        return memberProfileRepository.findAllByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public int countInCompany(Long companyId) {
        return memberProfileRepository.findAllByCompanyId(companyId).size();
    }

    @Transactional
    public void updateStatus(Long id, MemberStatus status) {
        requireById(id);
        memberProfileRepository.updateStatus(id, status);
    }
}
