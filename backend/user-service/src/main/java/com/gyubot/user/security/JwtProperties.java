package com.gyubot.user.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/*
 * auth-service가 발급한 JWT를 검증만 하기 위한 값 (issuer/secret은 auth-service와 반드시 동일해야 함).
 * user-service는 토큰을 발급하지 않으므로 TTL 등 발급 관련 값은 없다.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer) {
}
