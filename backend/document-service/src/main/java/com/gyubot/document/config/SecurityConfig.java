package com.gyubot.document.config;

import com.gyubot.document.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /*
     * 문서 관리(등록/목록/상세/수정/삭제)는 설계상 관리자 전용 메뉴라 ADMIN으로 막는다.
     * 다운로드·근거 문서 조회(citation)만은 예외 — 챗봇 답변의 "원문 확인"에서 임직원도
     * 호출하므로 로그인만 요구한다 (회사가 다른 문서는 companyId로 별도 확인).
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/error", "/swagger-ui/**", "/v3/api-docs/**")
                        .permitAll()
                        // /internal/**은 JWT가 아니라 X-Internal-Token으로 컨트롤러에서 직접 인증한다.
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/documents/*/download").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/documents/*/citation").authenticated()
                        .requestMatchers("/api/documents/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
