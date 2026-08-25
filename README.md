# GyuBot

사내 규정·문서 기반 AI 지식검색 서비스 (RAG). 울산 2반 구조 설계 리뷰(2026-08-20) 기준.

## 구조

모노레포로 프론트엔드와 백엔드(MSA) 서비스를 함께 관리합니다.

```
GyuBot/
├── frontend/              Vue 3 + Vite (client)
├── backend/
│   ├── discovery-service/ Eureka Server — 서비스 등록·탐색 (:8761)
│   ├── api-gateway/       Spring Cloud Gateway — 단일 진입점 (:8080)
│   ├── auth-service/      로그인·JWT·OTP (:8081)
│   ├── user-service/      (예정) 회원·마이페이지
│   ├── document-service/  (예정) 업로드·S3 저장
│   ├── search-service/    (예정) Chunking·벡터검색
│   └── chat-service/      (예정) RAG 대화 처리
├── infra/                 로컬 실행용 docker-compose (Kafka, Redis, DB 등)
└── docs/                  설계 발표자료, 요구사항정의서 등
```

## 로컬 실행

```bash
# 0. 인프라 먼저 기동 (MariaDB, Redis, Mailpit)
cd infra && docker compose up -d

# 1. Eureka
cd backend/discovery-service && ./gradlew bootRun

# 2. API Gateway
cd backend/api-gateway && ./gradlew bootRun

# 3. Auth Service
cd backend/auth-service && ./gradlew bootRun

# 4. 프론트엔드
cd frontend && npm install && npm run dev
```

Eureka 대시보드: http://localhost:8761
Mailpit(수신 메일 확인용 SMTP 캐처): http://localhost:8025

## infra/

`docker-compose.yml` 하나로 로컬 개발용 MariaDB(:3306, db `gyubot_auth`, root/root), Redis(:6379), Mailpit(SMTP :1025 / 웹 UI :8025)을 띄웁니다. 각 서비스의 `application.yml` 기본값이 이 구성과 그대로 맞게 되어 있어 별도 환경변수 설정 없이 바로 연결됩니다.

## auth-service 참고

- 포트 8081. `/actuator/health`에 `show-details: always`가 켜져 있어 DB·Redis·Mail·Eureka 연결 상태를 한 번에 확인할 수 있습니다 (인프라 기동 후 확인 완료: db/redis/mail 모두 UP, Eureka 등록도 확인됨).
- 아직 컨트롤러·엔티티·시큐리티 설정은 없는 순수 스캐폴드 상태입니다. `spring-boot-starter-security`가 붙어 있어 지금 그대로 뜨면 나머지 요청은 기본 로그인 화면으로 막힙니다 — `SecurityConfig` 작성이 다음 단계입니다.
