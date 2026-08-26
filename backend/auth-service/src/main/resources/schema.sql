-- 시스템 관리(SUPER_ADMIN 전용) 화면에서 관리하는 테넌트(회사) 목록. 회원가입 시 이메일 도메인으로
-- 소속 회사를 자동 판별하는 데도 쓰인다. 다른 서비스의 company_id는 이 테이블의 id를 참조하지만,
-- 이 프로젝트의 기존 관례대로 실제 FK 제약은 걸지 않는다(서비스 간 느슨한 결합 유지).
CREATE TABLE IF NOT EXISTS company (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email_domain VARCHAR(100) NOT NULL UNIQUE,
    tenant_code VARCHAR(30) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS auth_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id BIGINT NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
