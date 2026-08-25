package com.gyubot.document.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/*
 * auth-service가 발급한 JWT를 검증만 하기 위한 값 (issuer/secret은 auth-service와 반드시 동일해야 함).
 * document-service는 토큰을 발급하지 않는다.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer) {
}
