package com.gyubot.search.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/*
 * document-service의 내부 API를 호출해 원본 파일 바이트를 내려받는 클라이언트.
 * user-service의 AuthServiceClient와 동일한 패턴: @LoadBalanced RestClient.Builder는 Eureka
 * 클라이언트 자신의 내부 호출까지 로드밸런서를 태워 순환 참조를 일으키므로 쓰지 않고,
 * DiscoveryClient로 인스턴스를 직접 조회해 RestClient.create()로 만든 클라이언트로 호출한다.
 */
@Component
public class DocumentServiceClient {

    private static final String SERVICE_ID = "document-service";

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;
    private final String internalToken;

    public DocumentServiceClient(
            DiscoveryClient discoveryClient,
            @Value("${app.internal.token}") String internalToken) {
        this.discoveryClient = discoveryClient;
        this.restClient = RestClient.create();
        this.internalToken = internalToken;
    }

    public byte[] download(Long documentId) {
        String baseUrl = resolveBaseUrl();
        return restClient.get()
                .uri(baseUrl + "/internal/documents/{id}/download", documentId)
                .header("X-Internal-Token", internalToken)
                .retrieve()
                .body(byte[].class);
    }

    private String resolveBaseUrl() {
        List<ServiceInstance> instances = discoveryClient.getInstances(SERVICE_ID);
        if (instances.isEmpty()) {
            throw new IllegalStateException("Eureka에서 document-service 인스턴스를 찾을 수 없습니다.");
        }
        return instances.get(0).getUri().toString();
    }
}
