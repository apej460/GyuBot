package com.gyubot.gateway.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/*
 * 게이트웨이가 백엔드로 요청을 전달할 때 쓰는 RestClient(RestClientProxyExchange)는 기본값으로
 * Apache HttpClient5를 쓰는데, 이 클라이언트는 기본적으로 쿠키 저장소(BasicCookieStore)를 켠 채로
 * 만들어진다. 그러면 게이트웨이 자신이 백엔드 응답의 Set-Cookie를 저장했다가, 전혀 다른(심지어
 * 인증 안 된) 클라이언트의 다음 요청에 그 쿠키를 다시 붙여서 보내버린다 — 실제로 재현됨: A가
 * 로그인해서 받은 JWT 쿠키가, 쿠키 없이 요청한 B에게도 그대로 전달되는 세션 유출이 발생했다.
 * 게이트웨이는 순수 프록시라 요청마다 원래 클라이언트가 보낸 헤더를 그대로 전달하기만 하면 되고
 * 자체적으로 쿠키를 들고 있으면 안 되므로, 쿠키 관리를 완전히 꺼서 매 요청이 독립적이게 만든다.
 */
@Configuration
public class GatewayHttpClientConfig {

    @Bean
    public ClientHttpRequestFactory gatewayClientHttpRequestFactory() {
        CloseableHttpClient httpClient = HttpClients.custom()
                .disableCookieManagement()
                .build();
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
