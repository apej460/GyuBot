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
# 1. Eureka 먼저 기동
cd backend/discovery-service && ./gradlew bootRun

# 2. API Gateway
cd backend/api-gateway && ./gradlew bootRun

# 3. Auth Service (로컬 MariaDB·Redis 필요, infra/ 구성 전까지는 접속 정보를 환경변수로 맞춰줘야 함)
cd backend/auth-service && ./gradlew bootRun

# 4. 프론트엔드
cd frontend && npm install && npm run dev
```

Eureka 대시보드: http://localhost:8761

## auth-service 참고

- 포트 8081, `application.yml`의 `spring.datasource` / `spring.data.redis` / `spring.mail` 값은 전부 환경변수 오버라이드 가능한 개발용 기본값입니다. `infra/`에 docker-compose가 아직 없어서 로컬 MariaDB·Redis가 없으면 애플리케이션 자체는 뜨지만(연결은 지연 초기화) 실제 로그인/OTP API를 호출하는 시점에 에러가 납니다.
- 아직 컨트롤러·엔티티·시큐리티 설정은 없는 순수 스캐폴드 상태입니다. `spring-boot-starter-security`가 붙어 있어 지금 그대로 뜨면 모든 요청이 기본 로그인 화면으로 막힙니다 — `SecurityConfig` 작성이 다음 단계입니다.
