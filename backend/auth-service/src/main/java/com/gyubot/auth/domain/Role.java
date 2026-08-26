package com.gyubot.auth.domain;

public enum Role {
    EMPLOYEE,
    ADMIN,
    // 전체 회사(테넌트)를 관리하는 최상위 관리자. 시스템 관리(회사 등록) 화면 전용 권한이며,
    // 그 외에는 일반 ADMIN과 동일하게 취급한다 (JwtAuthenticationFilter가 ROLE_ADMIN도 함께 부여).
    SUPER_ADMIN
}
