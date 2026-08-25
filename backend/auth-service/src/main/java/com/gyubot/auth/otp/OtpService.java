package com.gyubot.auth.otp;

import com.gyubot.auth.exception.OtpVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class OtpService {

    private static final String KEY_PREFIX = "auth:otp:";

    private final SecureRandom random = new SecureRandom();
    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final Duration ttl;

    public OtpService(
            StringRedisTemplate redisTemplate,
            JavaMailSender mailSender,
            @Value("${app.otp.ttl}") Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.mailSender = mailSender;
        this.ttl = ttl;
    }

    public void issue(String email) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        redisTemplate.opsForValue().set(KEY_PREFIX + email, code, ttl);
        send(email, code);
    }

    public void verify(String email, String code) {
        String key = KEY_PREFIX + email;
        String expected = redisTemplate.opsForValue().get(key);
        if (expected == null || !expected.equals(code)) {
            throw new OtpVerificationException();
        }
        redisTemplate.delete(key);
    }

    private void send(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[GyuBot] 관리자 로그인 인증번호");
        message.setText("인증번호는 " + code + " 입니다. " + ttl.toMinutes() + "분 이내에 입력해주세요.");
        mailSender.send(message);
    }
}
