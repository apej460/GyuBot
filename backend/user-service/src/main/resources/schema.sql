CREATE TABLE IF NOT EXISTS member_profile (
    id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    department VARCHAR(50),
    position VARCHAR(30),
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS signup_request (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id BIGINT NOT NULL,
    email VARCHAR(120) NOT NULL,
    name VARCHAR(50) NOT NULL,
    company_name VARCHAR(100),
    department VARCHAR(50),
    position VARCHAR(30),
    password VARCHAR(100) NOT NULL,
    attachment_filename VARCHAR(255) NOT NULL,
    attachment_content_type VARCHAR(100) NOT NULL,
    attachment_path VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP NULL,
    PRIMARY KEY (id)
);
