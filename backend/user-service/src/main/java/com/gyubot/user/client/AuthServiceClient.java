package com.gyubot.user.client;

import com.gyubot.user.exception.DuplicateEmailException;
import com.gyubot.user.exception.OtpVerificationFailedException;
import com.gyubot.user.exception.PasswordChangeException;
import com.gyubot.user.exception.UnregisteredDomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

/*
 * auth-service의 내부 API를 호출하는 클라이언트. Eureka(DiscoveryClient)에서 auth-service
 * 인스턴스를 직접 조회해 호출하므로 IP·포트를 하드코딩하지 않는다.
 *
 * (@LoadBalanced RestClient.Builder를 빈으로 등록하는 방식은 시도했다가 걷어냈다: Spring Boot의
 * 기본 RestClient.Builder 빈을 대체해버려서 Eureka 클라이언트 자신의 내부 HTTP 호출까지
 * 로드밸런서를 타게 되고, 아직 뜨지 않은 자기 자신을 "discover"하려는 순환 참조로 부팅이
 * 실패했다. 그래서 여기서는 DiscoveryClient로 인스턴스를 직접 조회하고, RestClient.create()로
 * 만든 독립적인 클라이언트로 그 주소를 직접 호출한다.)
 */
@Component
public class AuthServiceClient {

    private static final String SERVICE_ID = "auth-service";

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;
    private final String internalToken;

    public AuthServiceClient(
            DiscoveryClient discoveryClient,
            @Value("${app.internal.token}") String internalToken) {
        this.discoveryClient = discoveryClient;
        this.restClient = RestClient.create();
        this.internalToken = internalToken;
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        String baseUrl = resolveBaseUrl();
        try {
            restClient.patch()
                    .uri(baseUrl + "/internal/auth-users/{id}/password", userId)
                    .header("X-Internal-Token", internalToken)
                    .body(new ChangePasswordPayload(currentPassword, newPassword))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new PasswordChangeException();
        }
    }

    /*
     * 일반 가입(회사 이메일 도메인 인증)에서 이메일 소유 확인용 OTP를 auth-service에 위임한다 —
     * auth-service가 이미 갖춘 Redis+메일 인프라를 그대로 재사용(중복 구현 방지).
     */
    public void issueOtp(String email, String purpose) {
        String baseUrl = resolveBaseUrl();
        restClient.post()
                .uri(baseUrl + "/internal/otp/issue")
                .header("X-Internal-Token", internalToken)
                .body(new OtpIssuePayload(email, purpose))
                .retrieve()
                .toBodilessEntity();
    }

    public void verifyOtp(String email, String code, String purpose) {
        String baseUrl = resolveBaseUrl();
        try {
            restClient.post()
                    .uri(baseUrl + "/internal/otp/verify")
                    .header("X-Internal-Token", internalToken)
                    .body(new OtpVerifyPayload(email, code, purpose))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new OtpVerificationFailedException();
        }
    }

    /*
     * 가입 승인 시 auth-service에 실제 로그인 계정을 만든다. encodedPassword는 가입 신청
     * 접수 시점에 이미 해시된 값이라 auth-service는 이를 재해시하지 않고 그대로 저장한다.
     */
    public Long createUser(Long companyId, String email, String encodedPassword, String name, String role) {
        String baseUrl = resolveBaseUrl();
        try {
            CreateUserResponse response = restClient.post()
                    .uri(baseUrl + "/internal/auth-users")
                    .header("X-Internal-Token", internalToken)
                    .body(new CreateUserPayload(companyId, email, encodedPassword, name, role))
                    .retrieve()
                    .body(CreateUserResponse.class);
            return response.id();
        } catch (HttpClientErrorException.Conflict e) {
            throw new DuplicateEmailException();
        }
    }

    /*
     * 이메일 도메인으로 등록된 회사를 찾는다. auth-service의 시스템 관리(회사 등록) 화면에서
     * 관리하는 목록을 그대로 조회하므로, 여기서 도메인 화이트리스트를 따로 들고 있지 않는다.
     */
    public CompanyInfo resolveCompanyByEmail(String email) {
        String baseUrl = resolveBaseUrl();
        try {
            return restClient.get()
                    .uri(baseUrl + "/internal/companies/resolve?email={email}", email)
                    .header("X-Internal-Token", internalToken)
                    .retrieve()
                    .body(CompanyInfo.class);
        } catch (HttpClientErrorException.BadRequest e) {
            throw new UnregisteredDomainException();
        }
    }

    public record CompanyInfo(Long id, String name, String emailDomain, String tenantCode, String status) {
    }

    private String resolveBaseUrl() {
        List<ServiceInstance> instances = discoveryClient.getInstances(SERVICE_ID);
        if (instances.isEmpty()) {
            throw new IllegalStateException("Eureka에서 auth-service 인스턴스를 찾을 수 없습니다.");
        }
        return instances.get(0).getUri().toString();
    }

    private record ChangePasswordPayload(String currentPassword, String newPassword) {
    }

    private record OtpIssuePayload(String email, String purpose) {
    }

    private record OtpVerifyPayload(String email, String code, String purpose) {
    }

    private record CreateUserPayload(Long companyId, String email, String encodedPassword, String name, String role) {
    }

    private record CreateUserResponse(Long id) {
    }
}
