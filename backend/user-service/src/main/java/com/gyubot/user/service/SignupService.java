package com.gyubot.user.service;

import com.gyubot.user.client.AuthServiceClient;
import com.gyubot.user.domain.MemberStatus;
import com.gyubot.user.domain.Role;
import com.gyubot.user.domain.SignupRequest;
import com.gyubot.user.domain.SignupStatus;
import com.gyubot.user.exception.DuplicateEmailException;
import com.gyubot.user.exception.DuplicatePendingSignupException;
import com.gyubot.user.exception.InvalidAttachmentException;
import com.gyubot.user.exception.InvalidSignupStatusException;
import com.gyubot.user.exception.SignupRequestNotFoundException;
import com.gyubot.user.repository.MemberProfileRepository;
import com.gyubot.user.repository.SignupRequestRepository;
import com.gyubot.user.storage.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
public class SignupService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "application/pdf");

    // 데모 스코프: 회사 선택 UI/테넌트 관리가 아직 없어 모든 신청을 기본 테넌트로 받는다.
    private static final Long DEFAULT_COMPANY_ID = 1L;

    private final SignupRequestRepository signupRequestRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final AuthServiceClient authServiceClient;
    private final JavaMailSender mailSender;

    public SignupService(
            SignupRequestRepository signupRequestRepository,
            MemberProfileRepository memberProfileRepository,
            PasswordEncoder passwordEncoder,
            FileStorageService fileStorageService,
            AuthServiceClient authServiceClient,
            JavaMailSender mailSender) {
        this.signupRequestRepository = signupRequestRepository;
        this.memberProfileRepository = memberProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
        this.authServiceClient = authServiceClient;
        this.mailSender = mailSender;
    }

    @Transactional
    public SignupRequest submit(
            String email, String name, String companyName, String department, String position,
            String rawPassword, MultipartFile attachment) {
        validateAttachment(attachment);
        if (memberProfileRepository.findByEmail(email).isPresent()) {
            throw new DuplicateEmailException();
        }
        if (signupRequestRepository.existsPendingByEmail(email)) {
            throw new DuplicatePendingSignupException();
        }

        FileStorageService.StoredFile stored = fileStorageService.store(attachment);
        String encodedPassword = passwordEncoder.encode(rawPassword);

        return signupRequestRepository.save(
                DEFAULT_COMPANY_ID, email, name, companyName, department, position, encodedPassword,
                stored.originalFilename(), attachment.getContentType(), stored.storedPath());
    }

    @Transactional(readOnly = true)
    public List<SignupRequest> listPending(Long companyId) {
        return signupRequestRepository.findAllByCompanyIdAndStatus(companyId, SignupStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public SignupRequest requireById(Long id) {
        return signupRequestRepository.findById(id).orElseThrow(SignupRequestNotFoundException::new);
    }

    public Resource loadAttachment(SignupRequest request) {
        return fileStorageService.load(request.attachmentPath());
    }

    @Transactional
    public void approve(Long id) {
        SignupRequest request = requireById(id);
        if (request.status() != SignupStatus.PENDING) {
            throw new InvalidSignupStatusException();
        }

        Long newUserId = authServiceClient.createUser(
                request.companyId(), request.email(), request.encodedPassword(), request.name(), "EMPLOYEE");
        memberProfileRepository.insert(
                newUserId, request.companyId(), request.email(), request.name(),
                request.department(), request.position(), Role.EMPLOYEE, MemberStatus.ACTIVE);
        signupRequestRepository.approve(id);

        sendMail(request.email(), "[GyuBot] 가입이 승인되었습니다", "가입 신청이 승인되었습니다. 이메일과 비밀번호로 로그인해주세요.");
    }

    @Transactional
    public void reject(Long id, String reason) {
        SignupRequest request = requireById(id);
        if (request.status() != SignupStatus.PENDING) {
            throw new InvalidSignupStatusException();
        }

        signupRequestRepository.reject(id, reason);
        sendMail(request.email(), "[GyuBot] 가입이 반려되었습니다", "가입 신청이 반려되었습니다.\n사유: " + reason);
    }

    private void validateAttachment(MultipartFile attachment) {
        if (attachment == null || attachment.isEmpty()) {
            throw new InvalidAttachmentException();
        }
        if (!ALLOWED_CONTENT_TYPES.contains(attachment.getContentType())) {
            throw new InvalidAttachmentException();
        }
    }

    private void sendMail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
