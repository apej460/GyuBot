package com.gyubot.search.search.web;

import com.gyubot.search.exception.InternalAuthException;
import com.gyubot.search.search.HybridSearchService;
import com.gyubot.search.search.dto.SearchResultResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
 * chat-service(RAG 답변 생성)가 근거 문서 청크를 찾기 위해 호출할 내부 전용 API.
 * chat-service가 아직 없어 지금은 curl로 직접 검증한다. 최종 사용자가 직접 호출하지 않으므로
 * JWT가 아니라 서비스 간 공유 시크릿(X-Internal-Token)으로 보호한다.
 */
@RestController
@RequestMapping("/internal/search")
public class InternalSearchController {

    private final HybridSearchService hybridSearchService;
    private final String internalToken;

    public InternalSearchController(HybridSearchService hybridSearchService, @Value("${app.internal.token}") String internalToken) {
        this.hybridSearchService = hybridSearchService;
        this.internalToken = internalToken;
    }

    @GetMapping
    public List<SearchResultResponse> search(
            @RequestParam Long companyId,
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK,
            @RequestHeader("X-Internal-Token") String token) {

        if (!internalToken.equals(token)) {
            throw new InternalAuthException();
        }
        return hybridSearchService.search(companyId, query, topK);
    }
}
