package com.gyubot.chat.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

/*
 * search-service의 하이브리드 검색 내부 API를 호출해 질문과 관련된 문서 청크를 가져온다.
 * user-service의 AuthServiceClient와 동일한 패턴: DiscoveryClient로 인스턴스를 직접 조회해
 * RestClient.create()로 만든 독립적인 클라이언트로 호출한다 (이유는 그 클래스의 주석 참고).
 */
@Component
public class SearchServiceClient {

    private static final String SERVICE_ID = "search-service";

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;
    private final String internalToken;

    public SearchServiceClient(
            DiscoveryClient discoveryClient,
            @Value("${app.internal.token}") String internalToken) {
        this.discoveryClient = discoveryClient;
        this.restClient = RestClient.create();
        this.internalToken = internalToken;
    }

    public List<SearchResultDto> search(Long companyId, String query, int topK) {
        String baseUrl = resolveBaseUrl();
        SearchResultDto[] results = restClient.get()
                .uri(baseUrl + "/internal/search?companyId={companyId}&query={query}&topK={topK}", companyId, query, topK)
                .header("X-Internal-Token", internalToken)
                .retrieve()
                .body(SearchResultDto[].class);
        return results == null ? List.of() : Arrays.asList(results);
    }

    private String resolveBaseUrl() {
        List<ServiceInstance> instances = discoveryClient.getInstances(SERVICE_ID);
        if (instances.isEmpty()) {
            throw new IllegalStateException("Eureka에서 search-service 인스턴스를 찾을 수 없습니다.");
        }
        return instances.get(0).getUri().toString();
    }
}
