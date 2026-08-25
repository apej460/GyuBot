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

- 포트 8081. `/actuator/health`에 `show-details: always`가 켜져 있어 DB·Redis·Mail·Eureka 연결 상태를 한 번에 확인할 수 있습니다.
- 로그인/JWT/OTP 구현 완료. 임직원(EMPLOYEE)은 이메일+비밀번호로 바로 로그인되고, 관리자(ADMIN)는 로그인 시 이메일로 OTP(6자리, 5분 TTL)를 받아 `/api/auth/otp/verify`로 한 번 더 인증해야 토큰이 발급됩니다 (규봇 설계상 "관리자 2차 인증"). JWT는 httpOnly 쿠키(`ACCESS_TOKEN`)로 내려가고, `REFRESH_TOKEN`은 Redis에 저장되어 `/api/auth/refresh` 호출 시 회전(rotate)됩니다.
- API: `POST /api/auth/login`, `POST /api/auth/otp/verify`, `POST /api/auth/refresh`, `POST /api/auth/logout`, `GET /api/auth/check`.
- 로컬 테스트 계정은 `DevDataSeeder`가 최초 기동 시 자동 생성합니다 (`employee@gyubot.local` / `admin@gyubot.local`, 비밀번호 `Passw0rd!`, company_id=1). 실제 회원가입/승인 플로우는 user-service 몫이라 auth-service에는 별도 가입 API가 없습니다 — 이건 로컬 개발/테스트 전용 시드 데이터입니다.
- 이메일/OTP는 로컬에서 Mailpit(http://localhost:8025)으로 실제 수신됩니다.
- 검증 완료(2026-08-25): employee 즉시 로그인 → `/check` 정상, admin 로그인 → OTP 메일 수신 → 인증 → 토큰 발급, OTP 재사용 차단, 잘못된 비밀번호 401, refresh 토큰 회전, logout 후 재요청 차단까지 전부 실제 인프라에 대해 curl로 확인.

## frontend 로그인 화면

`/login`(이메일/PW → 관리자면 OTP 입력 단계로 전환)과 `/`(로그인 정보 표시 + 로그아웃, `meta.requiresAuth`로 라우터 가드 보호)를 붙였습니다. 개발 서버는 `vite.config.js`의 `server.proxy`로 `/api/*` 요청을 `http://localhost:8081`(auth-service)로 그대로 넘겨서, CORS 설정 없이도 브라우저에서 httpOnly 쿠키가 자연스럽게 동작합니다 (나중에 api-gateway로 라우팅을 옮길 때는 프록시 타겟만 바꾸면 됨).

- 상태 관리: `src/stores/auth.js` (Pinia) — `login`, `verifyOtp`, `checkAuth`, `logout`
- API 클라이언트: `src/api/http.js` (axios, `withCredentials: true`)
- 검증 완료(2026-08-25, 실제 Chrome 브라우저로 확인): employee 로그인 → 홈 화면에 사용자 정보 표시 → 새로고침해도 세션 유지 → 로그아웃 → admin 로그인 → OTP 입력 화면 전환 → Mailpit에서 실제 수신한 인증번호 입력 → 인증 후 role=ADMIN으로 홈 화면 진입까지 전부 확인. 콘솔 에러 없음.
