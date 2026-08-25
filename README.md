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
│   ├── user-service/      회원·마이페이지 (:8082)
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

`docker-compose.yml` 하나로 로컬 개발용 MariaDB(:3306, root/root), Redis(:6379), Mailpit(SMTP :1025 / 웹 UI :8025)을 띄웁니다. 각 서비스의 `application.yml` 기본값이 이 구성과 그대로 맞게 되어 있어 별도 환경변수 설정 없이 바로 연결됩니다.

서비스마다 자기 DB를 따로 쓰는 database-per-service 구조라, `mariadb-init/001-create-databases.sql`이 컨테이너 최초 생성 시 `gyubot_auth`·`gyubot_user`를 함께 만듭니다. 이미 떠 있는 컨테이너에 새 서비스용 DB를 추가할 땐 init 스크립트가 다시 실행되지 않으니 `docker exec gyubot-mariadb mariadb -uroot -proot -e "CREATE DATABASE IF NOT EXISTS <db명>;"`로 수동 생성하고, 스크립트에도 같이 추가해 둘 것.

## auth-service 참고

- 포트 8081. `/actuator/health`에 `show-details: always`가 켜져 있어 DB·Redis·Mail·Eureka 연결 상태를 한 번에 확인할 수 있습니다.
- 로그인/JWT/OTP 구현 완료. 임직원(EMPLOYEE)은 이메일+비밀번호로 바로 로그인되고, 관리자(ADMIN)는 로그인 시 이메일로 OTP(6자리, 5분 TTL)를 받아 `/api/auth/otp/verify`로 한 번 더 인증해야 토큰이 발급됩니다 (규봇 설계상 "관리자 2차 인증"). JWT는 httpOnly 쿠키(`ACCESS_TOKEN`)로 내려가고, `REFRESH_TOKEN`은 Redis에 저장되어 `/api/auth/refresh` 호출 시 회전(rotate)됩니다.
- API: `POST /api/auth/login`, `POST /api/auth/otp/verify`, `POST /api/auth/refresh`, `POST /api/auth/logout`, `GET /api/auth/check`.
- 내부 전용 API도 있습니다: `PATCH /internal/auth-users/{id}/password` — user-service가 비밀번호 변경을 위임할 때만 호출하는 서비스 간 API로, JWT가 아니라 `X-Internal-Token` 헤더(양쪽 서비스가 공유하는 `app.internal.token`)로 보호됩니다. 최종 사용자가 직접 호출하는 API가 아닙니다.
- 로컬 테스트 계정은 `DevDataSeeder`가 최초 기동 시 자동 생성합니다 (`employee@gyubot.local` / `admin@gyubot.local`, 비밀번호 `Passw0rd!`, company_id=1). 실제 회원가입/승인 플로우는 user-service 몫이라 auth-service에는 별도 가입 API가 없습니다 — 이건 로컬 개발/테스트 전용 시드 데이터입니다.
- 이메일/OTP는 로컬에서 Mailpit(http://localhost:8025)으로 실제 수신됩니다.
- 검증 완료(2026-08-25): employee 즉시 로그인 → `/check` 정상, admin 로그인 → OTP 메일 수신 → 인증 → 토큰 발급, OTP 재사용 차단, 잘못된 비밀번호 401, refresh 토큰 회전, logout 후 재요청 차단까지 전부 실제 인프라에 대해 curl로 확인.

## frontend 화면

- `/login` — 이메일/PW, 관리자면 OTP 입력 단계로 전환
- `/` — 로그인 정보 표시, 내 정보·회원 관리(관리자만) 링크, 로그아웃 (`meta.requiresAuth`)
- `/mypage` — 내 정보 조회 + 비밀번호 변경 (`meta.requiresAuth`)
- `/admin/members` — 회원 목록 + 상태 변경(정지/활성화) 버튼, 관리자만 (`meta.requiresAuth`, `meta.requiresAdmin` — 라우터 가드에서 `auth.user.role !== 'ADMIN'`이면 홈으로 리다이렉트, 백엔드도 동일하게 403으로 막으므로 이중 방어)

개발 서버는 `vite.config.js`의 `server.proxy`로 `/api/auth/*`는 auth-service(:8081), `/api/users/*`는 user-service(:8082)로 각각 프록시합니다. 두 서비스 모두 `/api/...` 프리픽스를 쓰기 때문에 하나의 프록시 규칙으로 묶을 수 없어서 경로별로 나눴습니다 (api-gateway 라우팅이 갖춰지면 하나의 타겟으로 합치면 됨). 쿠키는 Vite가 프록시해주는 덕에 브라우저 입장에서는 항상 동일 출처(localhost:5173)라서 CORS 설정이 필요 없습니다.

- 상태 관리: `src/stores/auth.js`(로그인 세션) / `src/stores/member.js`(프로필·회원 관리, user-service 담당)
- API 클라이언트: `src/api/http.js` (axios, `withCredentials: true`)
- 검증 완료(2026-08-25, 실제 Chrome 브라우저로 확인 — claude-in-chrome): employee 로그인 → 홈 화면 정보 표시 → 새로고침해도 세션 유지 → 로그아웃 → admin 로그인 → OTP 입력 화면 전환 → Mailpit에서 실제 수신한 인증번호 입력 → role=ADMIN으로 홈 진입 → 내 정보 화면에서 잘못된 현재 비밀번호로 변경 시도 시 에러 표시 → 올바른 비밀번호로 변경 성공 표시 후 원래 비밀번호로 재변경까지 실제 폼 입력으로 확인 → 회원 관리 화면에서 상태 뱃지·버튼으로 정지/활성화 토글 확인 → employee 계정으로는 회원 관리 링크가 아예 안 보이고 URL 직접 접근 시에도 홈으로 리다이렉트되는 것까지 확인. 콘솔 에러 없음.
- 브라우저 자동화 테스트 팁: 이 앱을 claude-in-chrome으로 조작할 때 픽셀 좌표 클릭은 HiDPI 스크린샷 배율 때문에 가끔 엉뚱한 곳을 클릭합니다(입력 필드가 안 채워지거나 클릭이 씹힘) — `read_page`로 얻은 요소 ref로 클릭·입력하는 편이 훨씬 안정적입니다.

## user-service 참고

- 포트 8082. 자기 전용 DB(`gyubot_user`)를 씀 — auth-service와 DB를 공유하지 않는 database-per-service 구조.
- 마이페이지 조회 + 관리자용 회원 관리 구현 완료. auth-service가 발급한 JWT를 **검증만** 하고(같은 `app.jwt.secret`/`issuer` 공유), 직접 토큰을 발급하지는 않습니다.
- API: `GET /api/users/me`(본인 정보, 임직원/관리자 공통) · `PATCH /api/users/me/password`(비밀번호 변경) · `GET /api/users`(회사 소속 전체 목록, 관리자 전용, 테넌트 격리) · `GET /api/users/{id}`(상세, 관리자 전용) · `PATCH /api/users/{id}/status`(ACTIVE/SUSPENDED 변경, 관리자 전용).
- **서비스 간 연동으로 구현한 비밀번호 변경**: 실제 로그인 자격정보(`auth_user`)는 auth-service DB에만 있어서, user-service는 JWT로 본인 확인만 하고 실제 변경은 `AuthServiceClient`가 Eureka(`DiscoveryClient`)로 auth-service 인스턴스를 찾아 `PATCH /internal/auth-users/{id}/password`를 직접 호출하는 방식으로 위임합니다. (`@LoadBalanced RestClient.Builder` 빈으로 시도했다가, Eureka 클라이언트 자신의 내부 HTTP 호출까지 로드밸런서를 타면서 아직 뜨지 않은 자신을 discover하려는 순환 참조로 부팅이 실패해 — `DiscoveryClient`로 인스턴스를 직접 조회하는 방식으로 바꿨습니다.)
- 범위에서 제외한 것: **가입 승인(명함·재직증명서 업로드)**. document-service(파일 저장)가 아직 없어서 별도 단계로 남겨둠.
- `DevDataSeeder`가 auth-service와 **같은 id**(employee=1, admin=2)로 `member_profile`을 시드합니다. 실제로는 가입 승인 때 두 서비스의 계정이 함께 생겨야 하는데 그 연동이 없어서, 로컬 개발 편의상 여기서도 동일 계정을 직접 시드해 둔 것입니다.
- 검증 완료(2026-08-25): employee로 로그인해 `/api/users/me` 확인, 관리자 전용 API 호출 시 403 확인, admin으로 OTP 인증 후 회원 목록/상세 조회, 상태를 SUSPENDED로 변경 후 재조회로 반영 확인, 존재하지 않는 id 조회 시 404 확인, 잘못된 현재 비밀번호로 변경 시도 시 401, 올바른 현재 비밀번호로 변경 후 새 비밀번호로 실제 로그인 성공(구 비밀번호는 실패) 및 원복까지 확인, 내부 API에 잘못된/누락된 토큰으로 직접 호출 시 각각 403/400으로 거부되는 것도 확인 — 전부 실제 인프라에 대해 curl로 확인.
