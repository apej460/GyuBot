package com.gyubot.auth.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.gyubot.auth.domain.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final CookieService cookieService;
    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(CookieService cookieService, JwtProvider jwtProvider) {
        this.cookieService = cookieService;
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        cookieService.read(request, CookieService.ACCESS_TOKEN).ifPresent(token -> {
            try {
                DecodedJWT jwt = jwtProvider.verify(token);

                AuthPrincipal principal = new AuthPrincipal(
                        Long.valueOf(jwt.getSubject()),
                        jwt.getClaim("email").asString(),
                        jwt.getClaim("companyId").asLong(),
                        Role.valueOf(jwt.getClaim("role").asString()));

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        authoritiesFor(principal.role()));

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JWTVerificationException exception) {
                SecurityContextHolder.clearContext();
            }
        });

        filterChain.doFilter(request, response);
    }

    /*
     * SUPER_ADMIN은 시스템 관리 화면 전용 추가 권한이지, ADMIN이 하는 모든 걸 못 하게 되면 안 된다.
     * 그래서 SUPER_ADMIN에게는 ROLE_ADMIN도 같이 부여해 기존 hasRole("ADMIN") 검사를 그대로 통과시킨다.
     */
    private List<SimpleGrantedAuthority> authoritiesFor(Role role) {
        if (role == Role.SUPER_ADMIN) {
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
