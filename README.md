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
│   ├── document-service/  업로드·S3 저장 (:8083)
│   ├── search-service/    (예정) Chunking·벡터검색
│   └── chat-service/      (예정) RAG 대화 처리
├── infra/                 로컬 실행용 docker-compose (Kafka, Redis, DB 등)
└── docs/                  설계 발표자료, 요구사항정의서 등
```

## 로컬 실행

```bash
# 0. 인프라 먼저 기동 (MariaDB, Redis, Mailpit, Kafka)
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

`docker-compose.yml` 하나로 로컬 개발용 MariaDB(:3306, root/root), Redis(:6379), Mailpit(SMTP :1025 / 웹 UI :8025), Kafka(:9092, KRaft 단일 노드, Zookeeper 없음)를 띄웁니다. 각 서비스의 `application.yml` 기본값이 이 구성과 그대로 맞게 되어 있어 별도 환경변수 설정 없이 바로 연결됩니다.

서비스마다 자기 DB를 따로 쓰는 database-per-service 구조라, `mariadb-init/001-create-databases.sql`이 컨테이너 최초 생성 시 `gyubot_auth`·`gyubot_user`·`gyubot_document`를 함께 만듭니다. 이미 떠 있는 컨테이너에 새 서비스용 DB를 추가할 땐 init 스크립트가 다시 실행되지 않으니 `docker exec gyubot-mariadb mariadb -uroot -proot -e "CREATE DATABASE IF NOT EXISTS <db명>;"`로 수동 생성하고, 스크립트에도 같이 추가해 둘 것.

## auth-service 참고

- 포트 8081. `/actuator/health`에 `show-details: always`가 켜져 있어 DB·Redis·Mail·Eureka 연결 상태를 한 번에 확인할 수 있습니다.
- 로그인/JWT/OTP 구현 완료. 임직원(EMPLOYEE)은 이메일+비밀번호로 바로 로그인되고, 관리자(ADMIN)는 로그인 시 이메일로 OTP(6자리, 5분 TTL)를 받아 `/api/auth/otp/verify`로 한 번 더 인증해야 토큰이 발급됩니다 (규봇 설계상 "관리자 2차 인증"). JWT는 httpOnly 쿠키(`ACCESS_TOKEN`)로 내려가고, `REFRESH_TOKEN`은 Redis에 저장되어 `/api/auth/refresh` 호출 시 회전(rotate)됩니다.
- API: `POST /api/auth/login`, `POST /api/auth/otp/verify`, `POST /api/auth/refresh`, `POST /api/auth/logout`, `GET /api/auth/check`.
- 내부 전용 API도 있습니다 (JWT가 아니라 `X-Internal-Token` 헤더로 보호, 양쪽 서비스가 공유하는 `app.internal.token`, 최종 사용자가 직접 호출하지 않음): `PATCH /internal/auth-users/{id}/password`(user-service의 비밀번호 변경 위임) · `POST /internal/auth-users`(user-service의 가입 승인 처리에서 새 로그인 계정 생성, 이메일 중복 시 409).
- 로컬 테스트 계정은 `DevDataSeeder`가 최초 기동 시 자동 생성합니다 (`employee@gyubot.local` / `admin@gyubot.local`, 비밀번호 `Passw0rd!`, company_id=1). 실제 회원가입/승인 플로우는 user-service 몫이라 auth-service에는 별도 가입 API가 없습니다 — 이건 로컬 개발/테스트 전용 시드 데이터입니다.
- 이메일/OTP는 로컬에서 Mailpit(http://localhost:8025)으로 실제 수신됩니다.
- 검증 완료(2026-08-25): employee 즉시 로그인 → `/check` 정상, admin 로그인 → OTP 메일 수신 → 인증 → 토큰 발급, OTP 재사용 차단, 잘못된 비밀번호 401, refresh 토큰 회전, logout 후 재요청 차단까지 전부 실제 인프라에 대해 curl로 확인.

## frontend 화면

- `/login` — 이메일/PW, 관리자면 OTP 입력 단계로 전환
- `/` — 로그인 정보 표시, 내 정보·회원 관리(관리자만) 링크, 로그아웃 (`meta.requiresAuth`)
- `/mypage` — 내 정보 조회 + 비밀번호 변경 (`meta.requiresAuth`)
- `/signup` — 회사 이메일이 없는 사용자의 예외 가입 신청 (이메일/이름/비밀번호 + 명함·재직증명서 파일), 인증 불필요
- `/admin/members` — 회원 목록 + 상태 변경(정지/활성화) 버튼, 관리자만 (`meta.requiresAuth`, `meta.requiresAdmin` — 라우터 가드에서 `auth.user.role !== 'ADMIN'`이면 홈으로 리다이렉트, 백엔드도 동일하게 403으로 막으므로 이중 방어)
- `/admin/signup-requests` — 대기 중인 가입 신청 목록, 첨부파일 열람, 승인/반려(반려 사유 입력) — 관리자만

개발 서버는 `vite.config.js`의 `server.proxy`로 `/api/auth/*`는 auth-service(:8081), `/api/users/*`는 user-service(:8082)로 각각 프록시합니다. 두 서비스 모두 `/api/...` 프리픽스를 쓰기 때문에 하나의 프록시 규칙으로 묶을 수 없어서 경로별로 나눴습니다 (api-gateway 라우팅이 갖춰지면 하나의 타겟으로 합치면 됨). 쿠키는 Vite가 프록시해주는 덕에 브라우저 입장에서는 항상 동일 출처(localhost:5173)라서 CORS 설정이 필요 없습니다.

- 상태 관리: `src/stores/auth.js`(로그인 세션) / `src/stores/member.js`(프로필·회원 관리) / `src/stores/signup.js`(가입 신청 제출 + 관리자 승인/반려)
- API 클라이언트: `src/api/http.js` (axios, `withCredentials: true`)
- 검증 완료(2026-08-25, 실제 Chrome 브라우저로 확인 — claude-in-chrome): employee 로그인 → 홈 화면 정보 표시 → 새로고침해도 세션 유지 → 로그아웃 → admin 로그인 → OTP 입력 화면 전환 → Mailpit에서 실제 수신한 인증번호 입력 → role=ADMIN으로 홈 진입 → 내 정보 화면에서 잘못된 현재 비밀번호로 변경 시도 시 에러 표시 → 올바른 비밀번호로 변경 성공 표시 후 원래 비밀번호로 재변경까지 실제 폼 입력으로 확인 → 회원 관리 화면에서 상태 뱃지·버튼으로 정지/활성화 토글 확인 → employee 계정으로는 회원 관리 링크가 아예 안 보이고 URL 직접 접근 시에도 홈으로 리다이렉트되는 것까지 확인 → `/signup`에서 실제 파일(PNG) 첨부해 가입 신청 제출 → `/admin/signup-requests`에서 첨부파일 링크로 열람 → 반려(사유 입력) 후 Mailpit에서 반려 메일 확인 → 다시 신청 제출 후 승인 버튼 클릭 → 새 계정으로 실제 로그인 성공까지 전부 폼 조작으로 확인. 콘솔 에러 없음.
- 브라우저 자동화 테스트 팁: 이 앱을 claude-in-chrome으로 조작할 때 픽셀 좌표 클릭은 HiDPI 스크린샷 배율 때문에 가끔 엉뚱한 곳을 클릭합니다(입력 필드가 안 채워지거나 클릭이 씹힘) — `read_page`로 얻은 요소 ref로 클릭·입력하는 편이 훨씬 안정적입니다. 파일 입력은 `file_upload` 도구를 쓰고, 세션에 공유된 경로(스크래치패드 등)의 파일만 업로드할 수 있습니다.
- 백엔드 서비스를 재시작한 뒤 로그인이 브라우저에서만 500으로 실패하고 curl로는 잘 되는 경우가 있었습니다 — 오래 떠 있던 auth-service/user-service JVM이 반복적인 요청 처리 후 상태가 꼬이는 것으로 보이며, 원인을 더 파기보다는 **서비스를 재기동하는 쪽이 빠르고 확실**했습니다 (Vite 재시작은 무관했음). 로컬 개발 중 이런 증상이 보이면 먼저 의심할 것.

## user-service 참고

- 포트 8082. 자기 전용 DB(`gyubot_user`)를 씀 — auth-service와 DB를 공유하지 않는 database-per-service 구조.
- 마이페이지 조회 + 관리자용 회원 관리 구현 완료. auth-service가 발급한 JWT를 **검증만** 하고(같은 `app.jwt.secret`/`issuer` 공유), 직접 토큰을 발급하지는 않습니다.
- API: `GET /api/users/me`(본인 정보, 임직원/관리자 공통) · `PATCH /api/users/me/password`(비밀번호 변경) · `GET /api/users`(회사 소속 전체 목록, 관리자 전용, 테넌트 격리) · `GET /api/users/{id}`(상세, 관리자 전용) · `PATCH /api/users/{id}/status`(ACTIVE/SUSPENDED 변경, 관리자 전용).
- **서비스 간 연동으로 구현한 비밀번호 변경**: 실제 로그인 자격정보(`auth_user`)는 auth-service DB에만 있어서, user-service는 JWT로 본인 확인만 하고 실제 변경은 `AuthServiceClient`가 Eureka(`DiscoveryClient`)로 auth-service 인스턴스를 찾아 `PATCH /internal/auth-users/{id}/password`를 직접 호출하는 방식으로 위임합니다. (`@LoadBalanced RestClient.Builder` 빈으로 시도했다가, Eureka 클라이언트 자신의 내부 HTTP 호출까지 로드밸런서를 타면서 아직 뜨지 않은 자신을 discover하려는 순환 참조로 부팅이 실패해 — `DiscoveryClient`로 인스턴스를 직접 조회하는 방식으로 바꿨습니다.)
- `DevDataSeeder`가 auth-service와 **같은 id**(employee=1, admin=2)로 `member_profile`을 시드합니다. 실제 가입 승인 흐름과 별개로, 로컬 개발 편의상 여기서도 동일 계정을 직접 시드해 둔 것입니다.
- 검증 완료(2026-08-25): employee로 로그인해 `/api/users/me` 확인, 관리자 전용 API 호출 시 403 확인, admin으로 OTP 인증 후 회원 목록/상세 조회, 상태를 SUSPENDED로 변경 후 재조회로 반영 확인, 존재하지 않는 id 조회 시 404 확인, 잘못된 현재 비밀번호로 변경 시도 시 401, 올바른 현재 비밀번호로 변경 후 새 비밀번호로 실제 로그인 성공(구 비밀번호는 실패) 및 원복까지 확인, 내부 API에 잘못된/누락된 토큰으로 직접 호출 시 각각 403/400으로 거부되는 것도 확인 — 전부 실제 인프라에 대해 curl로 확인.

## document-service 참고

- 포트 8083. 자기 전용 DB(`gyubot_document`)를 씀 — database-per-service 구조.
- 현재는 discovery-service/api-gateway와 같은 수준의 순수 스캐폴드 상태(의존성 + `application.yml` + 부팅 확인만) — 업로드 컨트롤러·엔티티·S3 연동·Kafka 발행 로직은 아직 없음.
- 의존성은 미리 넣어뒀습니다: `software.amazon.awssdk:s3`(원본 파일 저장), `spring-boot-starter-kafka`(문서 업로드 완료 시 `document.uploaded` 이벤트 발행 — search-service가 이 이벤트를 구독해 비동기로 Chunking·색인을 수행하는 구조), auth-service가 발급한 JWT를 검증하기 위한 `app.jwt.secret`/`issuer`(user-service와 동일한 값).
- **Spring Boot 4.1부터 Kafka `HealthIndicator`가 기본 제공되지 않습니다** (매 헬스체크마다 `describeCluster()`를 호출하는 비용 때문에 액추에이터 기본에서 빠짐) — 다른 서비스들처럼 `/actuator/health`에서 실제 연결 상태를 바로 볼 수 있게 `health/KafkaHealthIndicator.java`를 직접 추가했습니다.
- Boot 4.1에서 Health/HealthIndicator 클래스의 패키지가 `org.springframework.boot.actuate.health`에서 `org.springframework.boot.health.contributor`로 이동했습니다 (별도 `spring-boot-health` 모듈로 분리됨) — 옛 패키지로 import하면 컴파일 에러가 납니다.
- 검증 완료(2026-08-25): `/actuator/health`에서 db(gyubot_document)·kafka·Eureka 전부 UP 확인, discovery-service에 `DOCUMENT-SERVICE`로 정상 등록 확인. S3는 실제 AWS 자격 증명이나 LocalStack이 없어 아직 연결 확인 전 — 업로드 구현 단계에서 붙일 예정.

## 가입 승인 플로우 (예외 가입)

REQ-F-002·003 기준 — 회사 이메일이 없는 사용자가 명함·재직증명서(JPG/PNG/PDF)를 첨부해 가입을 신청하면, 관리자가 검토해 승인/반려합니다.

- `POST /api/users/signup-requests` (multipart, 인증 불필요) — 이메일/이름/비밀번호/첨부파일 접수. 비밀번호는 접수 시점에 즉시 BCrypt로 해시해 저장하고 원문은 어디에도 남기지 않습니다. 이미 가입된 이메일(409), 이미 대기 중인 신청(409), 허용되지 않는 파일 형식(400)은 여기서 막습니다.
- `GET /api/users/signup-requests` · `GET /api/users/signup-requests/{id}` · `GET /api/users/signup-requests/{id}/attachment` — 관리자용 대기 목록/상세/첨부파일 열람 (`FileStorageService`가 로컬 디스크에 저장, `app.upload.dir` 기본값 `/tmp/gyubot-uploads` — document-service의 S3 저장이 생기면 그쪽 호출로 교체 예정).
- `POST /api/users/signup-requests/{id}/approve` — auth-service의 `POST /internal/auth-users`를 호출해 실제 로그인 계정을 만들고(비밀번호는 재해시 없이 그대로 전달), 같은 id로 `member_profile`도 만든 뒤 승인 메일을 보냅니다.
- `POST /api/users/signup-requests/{id}/reject` — 사유와 함께 반려 처리 후 반려 메일 발송. 이미 처리된 신청을 다시 승인/반려하면 409.
- 데모 스코프 한계: 회사 선택 UI/테넌트 관리가 없어 모든 신청을 `company_id=1`로 고정 접수합니다 (실제로는 신청 시점에 회사를 지정하거나 관리자가 배정해야 함).
- 검증 완료(2026-08-25, curl + 실제 Chrome 브라우저 모두): 중복 이메일/중복 대기/잘못된 파일 형식 각각의 거부, 승인 시 auth-service 계정 생성과 새 비밀번호로 실제 로그인 성공, 승인/반려 메일이 Mailpit에 정확한 내용으로 도착, 이미 처리된 신청 재처리 시 409.
