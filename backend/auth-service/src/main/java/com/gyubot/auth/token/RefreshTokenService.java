package com.gyubot.auth.token;

import com.gyubot.auth.exception.InvalidCredentialsException;
import com.gyubot.auth.security.JwtProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final String KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(StringRedisTemplate redisTemplate, JwtProperties jwtProperties) {
        this.redisTemplate = redisTemplate;
        this.jwtProperties = jwtProperties;
    }

    public String issue(Long userId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(KEY_PREFIX + token, String.valueOf(userId), jwtProperties.refreshTokenTtl());
        return token;
    }

    public Long resolve(String token) {
        String userId = redisTemplate.opsForValue().get(KEY_PREFIX + token);
        if (userId == null) {
            throw new InvalidCredentialsException();
        }
        return Long.valueOf(userId);
    }

    public void revoke(String token) {
        redisTemplate.delete(KEY_PREFIX + token);
    }
}
