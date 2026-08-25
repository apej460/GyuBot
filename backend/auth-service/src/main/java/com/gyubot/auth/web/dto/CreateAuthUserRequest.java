package com.gyubot.auth.web.dto;

import com.gyubot.auth.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/*
 * user-service의 가입 승인 처리에서 넘어오는 요청. encodedPassword는 이미 BCrypt로
 * 해시된 값이며 (원문 비밀번호는 어느 서비스 DB에도 저장하지 않는다), 여기서는
 * 재해시하지 않고 그대로 저장한다.
 */
public record CreateAuthUserRequest(
        @NotNull Long companyId,
        @NotBlank @Email String email,
        @NotBlank String encodedPassword,
        @NotBlank String name,
        @NotNull Role role
) {
}
