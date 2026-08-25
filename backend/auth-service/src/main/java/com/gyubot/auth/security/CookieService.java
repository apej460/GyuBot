package com.gyubot.auth.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
public class CookieService {

    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String REFRESH_TOKEN = "REFRESH_TOKEN";

    private final boolean secure;

    public CookieService(@Value("${app.cookie.secure:false}") boolean secure) {
        this.secure = secure;
    }

    public Optional<String> read(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    public ResponseCookie issue(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true).secure(secure).sameSite("Lax")
                .path("/").maxAge(maxAge).build();
    }

    public ResponseCookie delete(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true).secure(secure).sameSite("Lax")
                .path("/").maxAge(Duration.ZERO).build();
    }
}
