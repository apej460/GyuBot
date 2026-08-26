package com.gyubot.user.repository;

import com.gyubot.user.domain.SignupRequest;
import com.gyubot.user.domain.SignupStatus;

import java.util.List;
import java.util.Optional;

public interface SignupRequestRepository {
    Optional<SignupRequest> findById(Long id);

    List<SignupRequest> findAllByCompanyIdAndStatus(Long companyId, SignupStatus status);

    boolean existsPendingByEmail(String email);

    SignupRequest save(
            Long companyId,
            String email,
            String name,
            String companyName,
            String department,
            String position,
            String encodedPassword,
            String attachmentFilename,
            String attachmentContentType,
            String attachmentPath);

    void approve(Long id);

    void reject(Long id, String reason);
}
