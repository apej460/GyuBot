package com.gyubot.auth.web;

import com.gyubot.auth.exception.InternalAuthException;
import com.gyubot.auth.service.AuthService;
import com.gyubot.auth.web.dto.ChangePasswordRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * 다른 마이크로서비스(user-service 등)가 사용자를 대신해 호출하는 내부 전용 API.
 * 최종 사용자가 직접 호출하지 않으므로 JWT가 아니라 서비스 간 공유 시크릿(X-Internal-Token)으로 보호한다.
 */
@RestController
@RequestMapping("/internal/auth-users")
public class InternalAuthController {

    private final AuthService authService;
    private final String internalToken;

    public InternalAuthController(AuthService authService, @Value("${app.internal.token}") String internalToken) {
        this.authService = authService;
        this.internalToken = internalToken;
    }

    @PatchMapping("/{id}/password")
    public void changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestHeader("X-Internal-Token") String token) {

        if (!internalToken.equals(token)) {
            throw new InternalAuthException();
        }
        authService.changePassword(id, request.currentPassword(), request.newPassword());
    }
}
