package com.gyubot.chat.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.gyubot.chat.domain.Role;
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
    private final JWTVerifier verifier;

    public JwtAuthenticationFilter(CookieService cookieService, JwtProperties properties) {
        this.cookieService = cookieService;
        Algorithm algorithm = Algorithm.HMAC256(properties.secret());
        this.verifier = JWT.require(algorithm).withIssuer(properties.issuer()).build();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        cookieService.read(request, CookieService.ACCESS_TOKEN).ifPresent(token -> {
            try {
                DecodedJWT jwt = verifier.verify(token);

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
     * SUPER_ADMIN은 ADMIN이 하는 모든 걸 그대로 할 수 있어야 하므로 ROLE_ADMIN도 같이 부여한다.
     */
    private List<SimpleGrantedAuthority> authoritiesFor(Role role) {
        if (role == Role.SUPER_ADMIN) {
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
