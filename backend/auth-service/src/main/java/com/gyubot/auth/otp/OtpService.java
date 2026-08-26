package com.gyubot.auth.otp;

import com.gyubot.auth.exception.OtpVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/*
 * 관리자 로그인 2차 인증뿐 아니라 비밀번호 재설정, 회원가입 이메일 인증에도 재사용한다.
 * 같은 이메일이라도 목적(purpose)마다 Redis 키를 분리해 서로 간섭하지 않는다.
 */
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
        issue(email, OtpPurpose.ADMIN_LOGIN);
    }

    public void issue(String email, OtpPurpose purpose) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        redisTemplate.opsForValue().set(key(email, purpose), code, ttl);
        send(email, code, purpose);
    }

    public void verify(String email, String code) {
        verify(email, code, OtpPurpose.ADMIN_LOGIN);
    }

    public void verify(String email, String code, OtpPurpose purpose) {
        String key = key(email, purpose);
        String expected = redisTemplate.opsForValue().get(key);
        if (expected == null || !expected.equals(code)) {
            throw new OtpVerificationException();
        }
        redisTemplate.delete(key);
    }

    private String key(String email, OtpPurpose purpose) {
        return KEY_PREFIX + purpose.name() + ":" + email;
    }

    private void send(String email, String code, OtpPurpose purpose) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(purpose.subject());
        message.setText("인증번호는 " + code + " 입니다. " + ttl.toMinutes() + "분 이내에 입력해주세요.");
        mailSender.send(message);
    }
}
