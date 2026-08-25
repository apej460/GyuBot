package com.gyubot.user.client;

import com.gyubot.user.exception.DuplicateEmailException;
import com.gyubot.user.exception.PasswordChangeException;
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
     * 가입 승인 시 auth-service에 실제 로그인 계정을 만든다. encodedPassword는 가입 신청
     * 접수 시점에 이미 해시된 값이라 auth-service는 이를 재해시하지 않고 그대로 저장한다.
     */
    public Long createUser(Long companyId, String email, String encodedPassword, String name) {
        String baseUrl = resolveBaseUrl();
        try {
            CreateUserResponse response = restClient.post()
                    .uri(baseUrl + "/internal/auth-users")
                    .header("X-Internal-Token", internalToken)
                    .body(new CreateUserPayload(companyId, email, encodedPassword, name, "EMPLOYEE"))
                    .retrieve()
                    .body(CreateUserResponse.class);
            return response.id();
        } catch (HttpClientErrorException.Conflict e) {
            throw new DuplicateEmailException();
        }
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

    private record CreateUserPayload(Long companyId, String email, String encodedPassword, String name, String role) {
    }

    private record CreateUserResponse(Long id) {
    }
}
