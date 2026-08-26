CREATE TABLE IF NOT EXISTS document (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    version VARCHAR(20),
    category VARCHAR(50),
    -- 시행일이 오늘 이전/이후인지로 "사용 중"/"시행 예정" 상태를 계산해서 보여준다 — 별도 상태
    -- 컬럼을 두면 날짜를 바꿨을 때 상태가 안 맞게 될 수 있어, 상태는 저장하지 않고 매번 계산한다.
    effective_date DATE,
    revision_date DATE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
