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
│   ├── search-service/    Chunking·하이브리드 검색 (:8084)
│   └── chat-service/      RAG 대화 처리 (:8085)
├── infra/                 로컬 실행용 docker-compose (Kafka, Redis, DB 등)
└── docs/                  설계 발표자료, 요구사항정의서 등
```

## 로컬 실행

```bash
./scripts/run-all.sh   # infra -> 백엔드 7개(순차, 헬스체크 통과 후 다음 기동) -> 프론트엔드
./scripts/stop-all.sh  # 백엔드 7개 + 프론트엔드 중지 (infra 컨테이너는 유지)
./scripts/stop-all.sh --infra  # infra 컨테이너까지 함께 종료
```

각 서비스 로그는 `logs/<service-name>.log`에 쌓입니다. `run-all.sh`는 discovery-service부터 순서대로 띄우면서 매 서비스가 뜬 걸 확인(헬스체크)한 뒤 다음 서비스를 올립니다 — 위 "Eureka가 죽으면 나머지도 재기동" 문제를 피하려면 이 순서를 지키는 게 중요합니다.

Eureka 대시보드: http://localhost:8761
Mailpit(수신 메일 확인용 SMTP 캐처): http://localhost:8025

수동으로 하나씩 띄우고 싶을 때(디버깅 등):
```bash
cd infra && docker compose up -d
cd backend/discovery-service && ./gradlew bootRun   # 1
cd backend/api-gateway && ./gradlew bootRun          # 2
cd backend/auth-service && ./gradlew bootRun         # 3
cd backend/user-service && ./gradlew bootRun         # 4
cd backend/document-service && ./gradlew bootRun     # 5
cd backend/search-service && ./gradlew bootRun       # 6
cd backend/chat-service && ./gradlew bootRun         # 7
cd frontend && npm install && npm run dev            # 8
```

**Eureka(discovery-service)가 죽으면 나머지 서비스도 다시 띄워야 합니다.** 각 서비스의 Eureka 클라이언트는 등록/하트비트가 반복 실패하면 재시도를 포기하고, discovery-service가 다시 떠도 스스로 재등록하지 않습니다(장시간 로컬 개발 세션에서 메모리 압박 등으로 discovery-service만 죽는 경우가 실제로 있었음). `curl http://localhost:8761/eureka/apps`로 등록된 인스턴스 수를 확인해 6개(gateway/auth/user/document/search/chat) 미만이면, discovery-service를 먼저 올리고 나머지도 전부 재기동할 것 — 일부만 재기동하면 그 서비스만 복구되고 나머지는 계속 "Connection refused"로 실패합니다.

## infra/

`docker-compose.yml` 하나로 로컬 개발용 MariaDB(:3306, root/root), Redis(:6379), Mailpit(SMTP :1025 / 웹 UI :8025), Kafka(:9092, KRaft 단일 노드, Zookeeper 없음), MinIO(S3 호환 스토리지, API :9100 / 콘솔 :9101, minioadmin/minioadmin), Elasticsearch(:9200, 단일 노드, `xpack.security.enabled=false`로 로컬 전용 무인증 — 운영에서는 반드시 인증을 켜야 함), Ollama(:11434, search-service의 로컬 임베딩용)를 띄웁니다. 각 서비스의 `application.yml` 기본값이 이 구성과 그대로 맞게 되어 있어 별도 환경변수 설정 없이 바로 연결됩니다.

Elasticsearch 이미지 버전은 Boot가 관리하는 `co.elastic.clients:elasticsearch-java` 클라이언트 버전과 반드시 맞춰야 합니다(현재 9.4.5) — 버전이 안 맞으면 클라이언트 호출이 이상하게 실패할 수 있습니다.

Ollama는 컨테이너만 띄운다고 끝이 아니라 필요한 모델을 미리 받아둬야 합니다(최초 1회):
```bash
docker exec gyubot-ollama ollama pull nomic-embed-text   # search-service 임베딩용, 약 274MB
docker exec gyubot-ollama ollama pull qwen2.5:7b          # chat-service 답변 생성용, 약 4.7GB
```

MinIO 포트가 9000/9001이 아니라 9100/9101인 이유: 로컬에 Jupyter 커널이 떠 있으면 ZMQ 통신 채널 5개(shell/iopub/stdin/control/hb)가 9000~9004 포트를 통째로 점유합니다. 이 충돌 상태에서도 curl 요청은 그럴듯한 응답을 받아 정상처럼 보이지만, AWS SDK 클라이언트는 요청이 무한 대기하다 타임아웃됩니다 — 원인을 못 찾겠다면 `lsof -i -P -n -a -p <pid>`로 포트 점유 프로세스부터 확인할 것.

서비스마다 자기 DB를 따로 쓰는 database-per-service 구조라, `mariadb-init/001-create-databases.sql`이 컨테이너 최초 생성 시 `gyubot_auth`·`gyubot_user`·`gyubot_document`·`gyubot_chat`을 함께 만듭니다. 이미 떠 있는 컨테이너에 새 서비스용 DB를 추가할 땐 init 스크립트가 다시 실행되지 않으니 `docker exec gyubot-mariadb mariadb -uroot -proot -e "CREATE DATABASE IF NOT EXISTS <db명>;"`로 수동 생성하고, 스크립트에도 같이 추가해 둘 것.

## 관리자 콘솔 확장 (2026-08-26)

`docs/울산_2반_서준영_화면설계서.pages` 화면설계서를 기준으로 프론트엔드를 전면 재구성하고, 이를 뒷받침하는 백엔드 기능을 새로 추가했습니다 — 임직원 화면(AI 질의/질의 이력/내 정보)과 완전히 분리된 관리자 콘솔(대시보드/문서 관리/회원 관리/관리자 관리/시스템 관리)이 별도 레이아웃(다크 사이드바)으로 존재합니다.

- **`Role.SUPER_ADMIN` 추가**(auth/user/document/chat 4개 서비스 전부): `JwtAuthenticationFilter.authoritiesFor()`가 SUPER_ADMIN에게 `ROLE_ADMIN`과 `ROLE_SUPER_ADMIN` 권한을 동시에 부여해, 기존 `hasRole("ADMIN")` 규칙을 전혀 안 건드리고도 SUPER_ADMIN이 모든 관리자 API를 그대로 쓸 수 있습니다. SUPER_ADMIN 전용 기능(테넌트 관리, 다른 회사 관리자 조회 등)만 별도로 `hasRole("SUPER_ADMIN")` 또는 서비스 로직에서 `principal.role() == Role.SUPER_ADMIN`으로 구분합니다.
- **멀티테넌트 `company` 테이블**(auth-service, 신규): 회사명·이메일 도메인·테넌트 코드(`TEN-` + 6자리 난수)·상태(ACTIVE/SUSPENDED)를 관리합니다. `GET/POST /api/companies`는 SUPER_ADMIN 전용(SCR-SYS-001, `/admin/system`). 일반 가입(아래) 시 이메일 도메인으로 회사를 자동 매칭하는 데도 이 테이블을 씁니다.
- **일반 가입(회사 이메일 도메인 인증) 추가** — 기존 "예외 가입"(관리자 승인 필요, 명함/재직증명서 첨부)과 별개로, 등록된 도메인의 회사 이메일이면 승인 없이 즉시 가입되는 플로우를 새로 만들었습니다: `GET /api/users/signup/email/company?email=`(도메인으로 회사명 미리 확인) → `POST /api/users/signup/email/otp`(OTP 발송) → `POST /api/users/signup/email/complete`(OTP 검증 + 계정 즉시 활성화). `OtpService`를 `OtpPurpose`(ADMIN_LOGIN/PASSWORD_RESET/SIGNUP_EMAIL) enum으로 일반화해, 관리자 로그인 2차 인증과 동일한 Redis 기반 OTP 메커니즘을 3가지 용도로 재사용합니다(용도별로 Redis 키 네임스페이스 분리).
- **비밀번호 재설정(OTP 기반) 추가**: `POST /api/auth/password-reset/request` → 이메일로 OTP 발송, `POST /api/auth/password-reset/confirm` → 이메일+코드+새 비밀번호를 한 번에 검증·적용(별도의 "인증만" 하는 API가 없어 인증번호 오류도 이 마지막 호출에서 알 수 있습니다 — 프론트는 2단계 화면으로 나눠 보여주지만 실제 검증은 한 번에 일어남).
- **관리자 계정 관리와 회원 관리 분리**: `ManagerController`(`/api/users/managers`)가 ADMIN/SUPER_ADMIN 계정만 다루고, 기존 `MemberController`(`/api/users`)는 일반 회원만 다룹니다. SUPER_ADMIN이 아닌 ADMIN이 `role: "SUPER_ADMIN"`으로 계정을 만들려 하면 `InsufficientRoleException`(403)으로 막습니다 — 코드 리뷰 중 자체 발견한 권한 상승 허점을 커밋 전에 막은 것.
- **문서 버전 메타데이터 추가**(document-service): `version`/`category`/`effectiveDate`/`revisionDate` 컬럼을 `document` 테이블에 추가. 상태(`ACTIVE`/`UPCOMING`)는 **DB에 저장하지 않고** `DocumentResponse.statusOf()`에서 매번 `effectiveDate`와 오늘 날짜를 비교해 계산합니다 — 상태와 날짜가 어긋날 여지를 없애기 위한 선택.
- **문서 근거 조회 API 신설** — 챗봇 답변의 근거 문서 카드(AnswerSourceResponse)에는 `documentId`만 있고 버전/분류/시행일 같은 메타데이터가 없어서, 임직원이 답변 화면에서 이 정보를 볼 방법이 없었습니다. 기존 문서 상세(`GET /api/documents/{id}`)는 관리자 전용이라 그대로 열 수 없었고, companyId 확인도 없어 그대로 열면 다른 회사 문서까지 보일 위험이 있었습니다. 그래서 `GET /api/documents/{id}/citation`을 새로 추가했습니다 — 로그인만 있으면 호출 가능하지만 `DocumentService.requireByIdForCompany()`로 요청자의 companyId와 문서의 companyId를 반드시 비교해, 다른 회사 문서는 id를 안다고 해도 404로 막습니다(다운로드 API와 동일한 패턴, REQ-F-018).
- **운영 대시보드 통계**: 전용 집계 서비스 없이 각 서비스에 `/stats` 엔드포인트를 하나씩 추가하는 방식을 택했습니다 — `GET /api/users/stats`(전체 회원 수), `GET /api/documents/stats`(등록 문서 수), `GET /api/chat/stats`(금일 질의 수·미답변 질문 수·최근 7일 질의량·자주 검색된 키워드 top4). 키워드 추출(`KeywordExtractor`)은 형태소 분석기 없이 공백/구두점 기준 단어 빈도 계산 + 소규모 한국어 불용어 목록만 쓰는 단순한 방식입니다 — "요즘 뭘 많이 물어보는지" 정도의 대시보드 위젯 용도로는 이 정도로 충분하다고 판단했습니다.
- **`api-gateway` 라우트 누락 버그**: `CompanyController`(`/api/companies/**`)를 auth-service에 추가하면서 게이트웨이 `application.yml`에 대응 라우트 추가를 빠뜨려, 프론트에서 시스템 관리 화면 진입 시 404가 났습니다. `auth-service-companies` 라우트(`Path=/api/companies/**` → `lb://auth-service`)를 추가해 해결 — **새 컨트롤러를 만들 때마다 게이트웨이 라우트도 같이 추가했는지 체크리스트로 확인할 것.**
- 프론트엔드는 라우트 메타(`meta.requiresAdmin`/`meta.requiresSuperAdmin`)로 레이아웃을 3단계로 나눕니다: 로그인 전(카드만), 임직원(`EmployeeShell` — 밝은 헤더), 관리자(`AdminShell` — 다크 사이드바, "시스템 관리" 메뉴는 SUPER_ADMIN에게만 표시). 신규 Pinia 스토어: `emailSignup.js`/`manager.js`/`company.js`/`passwordReset.js`/`adminStats.js`/`adminDocuments.js`.
- **검증 완료(2026-08-26, 실제 인프라 + 실제 Chrome 브라우저)**: superadmin OTP 로그인 → 대시보드 실데이터(회원/문서/질의 수, 7일 차트, 키워드) 확인 → 시스템 관리에서 회사 목록 조회·신규 회사 등록 → 관리자 관리에서 목록·추가 다이얼로그 확인 → 문서 관리에서 버전/분류/시행일 포함 문서 등록 후 목록 반영·삭제까지 확인 → 회원 관리(활성 회원/가입 승인 대기 탭) 확인 → 임직원 로그인 → AI 질의로 실제 RAG 답변 수신 → 근거 문서 카드 클릭 시 우측 드로어에 버전/분류/시행일/상태+발췌+다운로드 버튼 표시(새 `/citation` API로 확인) → 질의 이력 검색/날짜 필터 → 내 정보 화면 → 비밀번호 재설정 마법사(이메일 인증 → 새 비밀번호 → 완료, 이후 원래 비밀번호로 재복원) → 일반 이메일 가입 마법사(도메인 인증 → OTP → 프로필 입력 → 즉시 가입 완료 → 승인 대기 없이 바로 로그인 성공)까지 전부 확인.

## api-gateway 참고

- 포트 8080. Spring Cloud Gateway의 MVC(서블릿, 블로킹) 구현체(`spring-cloud-starter-gateway-server-webmvc`)를 씁니다 — 나머지 서비스가 전부 WebFlux가 아니라 서블릿 기반(JdbcTemplate, 동기 RestClient)이라 그 스타일에 맞춘 선택입니다.
- 라우트는 전부 `application.yml`에 명시적으로 정의합니다: `/api/auth/**` → auth-service, `/api/users/**` → user-service, `/api/documents/**` → document-service, `/api/chat/**` → chat-service. 각 서비스의 JWT/역할 검증은 그대로 각 서비스가 하고, 게이트웨이는 순수 라우팅만 합니다.
- **`spring.cloud.gateway.server.webmvc.discovery.locator.enabled=false`로 명시적으로 꺼뒀습니다.** 켜두면 Eureka에 등록된 서비스마다 `/AUTH-SERVICE/**`, `/SEARCH-SERVICE/**`처럼 서비스명 그대로의 경로를 자동으로 만드는데, 그러면 `X-Internal-Token`으로만 보호된 내부 API(search-service의 `/internal/search` 등)가 이 게이트웨이를 통해 인증 없이 그대로 외부에 노출될 뻔했습니다. search-service는 아예 공개 라우트가 없습니다 — 다른 서비스가 내부적으로만 호출하는 구조라 게이트웨이로 노출할 이유가 없습니다.
- **발견한 보안 버그와 수정**: Spring Cloud Gateway MVC가 프록시 요청에 기본으로 쓰는 `RestClient`는 내부적으로 Apache HttpClient5를 쓰는데, 이 클라이언트가 **기본값으로 쿠키 저장소(cookie store)를 켠 채** 만들어집니다. 그 결과 게이트웨이 자신이 백엔드 응답의 `Set-Cookie`(로그인 시 발급되는 JWT)를 저장해뒀다가, 완전히 다른(쿠키 없이 요청한) 클라이언트의 다음 요청에도 그 쿠키를 그대로 붙여서 전달해버리는 **세션 유출**이 실제로 재현됐습니다 (A가 로그인 → 곧바로 쿠키 없이 B가 같은 API를 호출해도 A의 데이터가 그대로 반환됨). `config/GatewayHttpClientConfig.java`에서 `HttpClients.custom().disableCookieManagement()`로 만든 `ClientHttpRequestFactory` 빈을 등록해 해결했습니다 — 게이트웨이가 만드는 `RestClientCustomizer`가 이 빈을 자동으로 집어가 프록시용 RestClient에 적용합니다. 게이트웨이는 순수 프록시라 원래 클라이언트가 보낸 헤더만 그대로 전달하면 되고 자체적으로 쿠키를 들고 있으면 안 되므로, 매 요청이 완전히 독립적이도록 쿠키 관리 자체를 껐습니다.
- **잠깐 겪은 별개 문제**: 오래 떠 있던 auth-service/user-service가 Eureka에 예전 IP(맥 네트워크 인터페이스가 그사이 바뀌기 전 IP)로 등록된 채로 남아있어서, 게이트웨이가 그 주소로 프록시하려다 "Network is unreachable"로 실패했습니다. 두 서비스를 재기동해서 현재 IP로 다시 등록하니 바로 해결됨 — **오래 켜둔 서비스가 갑자기 다른 서비스로부터의 요청만 실패한다면(직접 호출은 되는데) Eureka에 등록된 IP가 최신인지부터 의심할 것.**
- 검증 완료(2026-08-26): 로그인·내 정보·AI 질의·문서 목록(관리자) 전부 게이트웨이(:8080)를 통해 정상 동작, 관리자 전용 API에 직원 계정으로 접근 시 403 그대로 전달, search-service 내부 API와 discovery-locator 스타일 raw 경로 모두 404로 막힘, 로그인 직후 쿠키 없는 요청이 더 이상 세션을 가로채지 않는 것까지 확인. 프론트엔드도 Vite 프록시를 게이트웨이 하나로 합친 뒤 실제 Chrome 브라우저로 AI 질의 화면이 그대로 동작하는 것까지 확인.

## auth-service 참고

- 포트 8081. `/actuator/health`에 `show-details: always`가 켜져 있어 DB·Redis·Mail·Eureka 연결 상태를 한 번에 확인할 수 있습니다.
- 로그인/JWT/OTP 구현 완료. 임직원(EMPLOYEE)은 이메일+비밀번호로 바로 로그인되고, 관리자(ADMIN)는 로그인 시 이메일로 OTP(6자리, 5분 TTL)를 받아 `/api/auth/otp/verify`로 한 번 더 인증해야 토큰이 발급됩니다 (규봇 설계상 "관리자 2차 인증"). JWT는 httpOnly 쿠키(`ACCESS_TOKEN`)로 내려가고, `REFRESH_TOKEN`은 Redis에 저장되어 `/api/auth/refresh` 호출 시 회전(rotate)됩니다.
- API: `POST /api/auth/login`, `POST /api/auth/otp/verify`, `POST /api/auth/refresh`, `POST /api/auth/logout`, `GET /api/auth/check`.
- 내부 전용 API도 있습니다 (JWT가 아니라 `X-Internal-Token` 헤더로 보호, 양쪽 서비스가 공유하는 `app.internal.token`, 최종 사용자가 직접 호출하지 않음): `PATCH /internal/auth-users/{id}/password`(user-service의 비밀번호 변경 위임) · `POST /internal/auth-users`(user-service의 가입 승인 처리에서 새 로그인 계정 생성, 이메일 중복 시 409).
- 로컬 테스트 계정은 `DevDataSeeder`가 최초 기동 시 자동 생성합니다 (`employee@gyubot.local` / `admin@gyubot.local`, 비밀번호 `Passw0rd!`, company_id=1). 실제 회원가입/승인 플로우는 user-service 몫이라 auth-service에는 별도 가입 API가 없습니다 — 이건 로컬 개발/테스트 전용 시드 데이터입니다.
- 이메일/OTP는 로컬에서 Mailpit(http://localhost:8025)으로 실제 수신됩니다.
- 검증 완료(2026-08-25): employee 즉시 로그인 → `/check` 정상, admin 로그인 → OTP 메일 수신 → 인증 → 토큰 발급, OTP 재사용 차단, 잘못된 비밀번호 401, refresh 토큰 회전, logout 후 재요청 차단까지 전부 실제 인프라에 대해 curl로 확인.

## frontend 화면

화면설계서(`docs/울산_2반_서준영_화면설계서.pages`) 기준으로 임직원 화면과 관리자 콘솔을 완전히 분리했습니다. `router/index.js`가 `meta.requiresAdmin`/`meta.requiresSuperAdmin`으로 접근을 가드하고, `App.vue`가 같은 메타로 레이아웃(`EmployeeShell`/`AdminShell`/로그인 카드)을 고릅니다.

**인증**
- `/login` — 임직원 로그인, 관리자는 `/admin/login`으로 별도 진입점을 씁니다.
- `/admin/login` — 관리자 로그인(다크 테마), 로그인 성공 시 OTP 2차 인증 단계로 전환.
- `/reset-password` — 비밀번호 재설정 마법사(이메일 인증 → 새 비밀번호 → 완료).
- `/signup/email` — 회사 이메일 도메인 인증으로 즉시 가입(승인 불필요): 이메일+OTP+비밀번호 → 이름/소속회사(읽기전용)/부서/직급 → 완료.
- `/signup/document` — 회사 이메일이 없는 사용자의 예외 가입 신청(이메일/이름/회사명/부서/직급/비밀번호 + 명함·재직증명서 첨부), 관리자 승인 필요.

**임직원** (`EmployeeShell`, `meta.requiresAuth`)
- `/chat` (`/chat/:id`) — AI 질의. 대화가 없으면 예시 질문 칩을 보여주고, 답변에는 "근거 문서 N개 문서에서 답변을 확인했어요" 문구와 함께 클릭 가능한 근거 문서 카드가 붙습니다. 카드를 클릭하면 우측 드로어가 열려 `GET /api/documents/{id}/citation`으로 버전/분류/시행일/개정일/상태와 발췌, "원본 문서 다운로드" 버튼을 보여줍니다.
- `/history` — 질의 이력. 질문 검색 + 날짜 범위 필터(클라이언트 사이드).
- `/mypage` — 내 정보(이름/이메일/부서/직급/역할/상태) + 비밀번호 변경.

**관리자 콘솔** (`AdminShell`, `meta.requiresAuth` + `requiresAdmin`/`requiresSuperAdmin`)
- `/admin/dashboard` — 통계 카드 4개(전체 회원/등록 문서/금일 질의/미답변 질문) + 최근 7일 질의량(CSS 바 차트, 외부 차트 라이브러리 없이 직접 구현) + 자주 검색된 키워드 목록.
- `/admin/documents`, `/admin/documents/new` — 문서 목록(검색, 버전/분류/시행일/상태) 및 등록(메타데이터 + 파일 업로드).
- `/admin/members` — 활성 회원 탭 + 가입 승인 대기 탭을 하나로 합친 화면(기존 별도 화면 두 개를 병합).
- `/admin/managers` — 관리자 계정 목록 + 추가(SUPER_ADMIN만 SUPER_ADMIN 역할 부여 가능).
- `/admin/system` — **SUPER_ADMIN 전용**. 회사(테넌트) 목록 조회 + 등록.

개발 서버는 `vite.config.js`의 `server.proxy`로 `/api/*` 전체를 api-gateway(:8080) 하나로만 프록시합니다 — 서비스별 포트를 프론트가 알 필요 없이, 실제 라우팅은 게이트웨이가 맡습니다. 쿠키는 Vite가 프록시해주는 덕에 브라우저 입장에서는 항상 동일 출처라서 CORS 설정이 필요 없습니다.

- 상태 관리: `src/stores/auth.js`(로그인 세션) / `src/stores/member.js`(프로필·회원 관리) / `src/stores/signup.js`(예외 가입 제출 + 관리자 승인/반려) / `src/stores/emailSignup.js`(일반 가입) / `src/stores/passwordReset.js` / `src/stores/manager.js` / `src/stores/company.js` / `src/stores/adminStats.js` / `src/stores/adminDocuments.js` / `src/stores/chat.js`(질문 전송·대화 이력·근거 문서 조회)
- API 클라이언트: `src/api/http.js` (axios, `withCredentials: true`)
- UI는 element-plus로 구성되어 있습니다(`main.js`에서 `app.use(ElementPlus)`). 새 아이콘 패키지 없이 el-card/el-table/el-form/el-tag/el-descriptions/el-drawer/el-dialog/el-tabs 등 기본 컴포넌트만으로 화면을 구성했습니다.
- **검증 완료(2026-08-26, 관리자 콘솔 확장 후 재검증, 실제 Chrome 브라우저)**: 위 "관리자 콘솔 확장" 절 참고.
- **브라우저 자동화 테스트 팁**: claude-in-chrome으로 이 앱을 조작할 때 픽셀 좌표 클릭은 페이지가 미세하게 리플로우(레이아웃 shift)될 때마다 어긋나기 쉽습니다(입력 필드가 안 채워지거나 클릭이 씹힘, 매 상호작용 전 새 스크린샷으로 좌표를 다시 잡아야 함). `read_page`/`find`로 얻은 요소 ref로 클릭·입력하는 편이 대체로 더 안정적이지만, **이번 세션에서는 특정 `native-type="submit"` el-button에서 ref 클릭도 좌표 클릭도 전혀 반응하지 않는 경우(네트워크 요청 자체가 안 나감, 콘솔 에러도 없음)가 있었습니다** — `javascript_tool`로 `document.querySelectorAll('button')`에서 텍스트로 버튼을 찾아 `.click()`을 직접 호출하니 즉시 정상 동작했습니다. 커스텀 컴포넌트 버튼이 사람 조작을 흉내 낸 합성 클릭에 반응하지 않을 때는 이 방법으로 우회할 것 — 실제 앱 버그가 아니라 자동화 도구의 클릭 전달 문제였음을 `.click()` 직접 호출 성공으로 확인했습니다. 파일 입력은 `file_upload` 도구를 쓰고, 세션에 공유된 경로(스크래치패드 등)의 파일만 업로드할 수 있습니다.
- 백엔드 서비스를 재시작한 뒤 로그인이 브라우저에서만 500으로 실패하고 curl로는 잘 되는 경우가 있었습니다 — 오래 떠 있던 auth-service/user-service JVM이 반복적인 요청 처리 후 상태가 꼬이는 것으로 보이며, 원인을 더 파기보다는 **서비스를 재기동하는 쪽이 빠르고 확실**했습니다 (Vite 재시작은 무관했음). 로컬 개발 중 이런 증상이 보이면 먼저 의심할 것.

## user-service 참고

- 포트 8082. 자기 전용 DB(`gyubot_user`)를 씀 — auth-service와 DB를 공유하지 않는 database-per-service 구조.
- 마이페이지 조회 + 관리자용 회원 관리 구현 완료. auth-service가 발급한 JWT를 **검증만** 하고(같은 `app.jwt.secret`/`issuer` 공유), 직접 토큰을 발급하지는 않습니다.
- API: `GET /api/users/me`(본인 정보, 임직원/관리자 공통) · `PATCH /api/users/me/password`(비밀번호 변경) · `GET /api/users`(회사 소속 전체 목록, 관리자 전용, 테넌트 격리) · `GET /api/users/{id}`(상세, 관리자 전용) · `PATCH /api/users/{id}/status`(ACTIVE/SUSPENDED 변경, 관리자 전용).
- **서비스 간 연동으로 구현한 비밀번호 변경**: 실제 로그인 자격정보(`auth_user`)는 auth-service DB에만 있어서, user-service는 JWT로 본인 확인만 하고 실제 변경은 `AuthServiceClient`가 Eureka(`DiscoveryClient`)로 auth-service 인스턴스를 찾아 `PATCH /internal/auth-users/{id}/password`를 직접 호출하는 방식으로 위임합니다. (`@LoadBalanced RestClient.Builder` 빈으로 시도했다가, Eureka 클라이언트 자신의 내부 HTTP 호출까지 로드밸런서를 타면서 아직 뜨지 않은 자신을 discover하려는 순환 참조로 부팅이 실패해 — `DiscoveryClient`로 인스턴스를 직접 조회하는 방식으로 바꿨습니다.)
- `DevDataSeeder`가 auth-service와 **같은 id**(employee=1, admin=2)로 `member_profile`을 시드합니다. 실제 가입 승인 흐름과 별개로, 로컬 개발 편의상 여기서도 동일 계정을 직접 시드해 둔 것입니다.
- 검증 완료(2026-08-25): employee로 로그인해 `/api/users/me` 확인, 관리자 전용 API 호출 시 403 확인, admin으로 OTP 인증 후 회원 목록/상세 조회, 상태를 SUSPENDED로 변경 후 재조회로 반영 확인, 존재하지 않는 id 조회 시 404 확인, 잘못된 현재 비밀번호로 변경 시도 시 401, 올바른 현재 비밀번호로 변경 후 새 비밀번호로 실제 로그인 성공(구 비밀번호는 실패) 및 원복까지 확인, 내부 API에 잘못된/누락된 토큰으로 직접 호출 시 각각 403/400으로 거부되는 것도 확인 — 전부 실제 인프라에 대해 curl로 확인.

## document-service 참고

- 포트 8083. 자기 전용 DB(`gyubot_document`)를 씀 — database-per-service 구조. auth-service가 발급한 JWT를 검증만 하며(user-service와 동일한 `JwtAuthenticationFilter` 패턴), 등록·목록·수정·삭제는 관리자 전용입니다(설계상 "문서 관리"는 관리자 메뉴).
- API: `POST /api/documents`(multipart, 등록, 관리자) · `GET /api/documents`(목록, 회사 소속만, 관리자) · `GET /api/documents/{id}`(상세, 관리자) · `PATCH /api/documents/{id}`(제목 수정, 관리자) · `DELETE /api/documents/{id}`(삭제, 관리자) · `GET /api/documents/{id}/download`(원본 다운로드, **임직원·관리자 공통**).
- **다운로드만 임직원에게도 열려 있습니다** — 챗봇 답변의 "원문 확인"(REQ-F-006)에서 호출되므로 로그인만 요구합니다. 대신 `DocumentService.download(id, companyId)`가 문서의 companyId와 요청자의 companyId를 반드시 비교해서, 다른 회사 문서는 id를 안다고 해도 404로 막습니다(REQ-F-018). 문서 보안등급에 따른 다운로드 제한(REQ-F-008)은 아직 없습니다 — 문서에 등급 필드 자체가 없어서 나중에 등급 체계를 만들 때 같이 다룰 예정입니다.
- REQ-F-012 기준 검증: 파일당 최대 50MB, PDF·HWP만 허용(확장자 기준 — HWP는 브라우저가 보고하는 content-type이 제각각이라 신뢰하지 않음).
- **업로드 → S3 저장 → `document.uploaded` Kafka 이벤트 발행**까지 한 번에 처리합니다 (`DocumentService.upload()`). 삭제 시에는 S3 원본도 함께 지우고 `document.deleted` 이벤트를 발행합니다 — 두 이벤트 다 search-service가 구독해 벡터 색인을 만들거나 지우는 데 씁니다. S3 클라이언트는 로컬 개발에서 MinIO(S3 호환)를 바라보고, `app.s3.endpoint`를 비우면 실제 AWS로 그대로 전환됩니다.
- `GET /internal/documents/{id}/download` — search-service가 Chunking을 위해 원본 파일을 내려받는 내부 전용 API. auth-service의 internal API와 동일하게 JWT가 아니라 `X-Internal-Token` 헤더로 보호합니다(`app.internal.token`, search-service와 같은 값이어야 함).
- Spring Boot 4.1 관련 메모 두 가지: (1) Kafka `HealthIndicator`가 더 이상 기본 제공되지 않아(매 헬스체크마다 `describeCluster()` 호출 비용 때문) `health/KafkaHealthIndicator.java`를 직접 추가했습니다. (2) `Health`/`HealthIndicator` 클래스가 `org.springframework.boot.actuate.health`에서 `org.springframework.boot.health.contributor`(신설된 `spring-boot-health` 모듈)로 이동했습니다 — 옛 패키지로 import하면 컴파일 에러.
- **가장 오래 걸린 삽질**: MinIO에 대한 모든 S3 요청이 AWS SDK for Java v2에서만 무한 대기하다 타임아웃되는 문제가 있었습니다(curl·AWS CLI·Python botocore는 전부 즉시 성공). MinIO 버전 문제(체크섬/청크 인코딩), IPv4/IPv6, HTTP 클라이언트 구현체(Apache vs `UrlConnectionHttpClient`) 순으로 의심하고 다 시도해봤지만 전부 아니었고, `lsof -i -P -n -a -p <pid>`로 확인해보니 **로컬에 떠 있던 Jupyter 커널이 ZMQ 채널 5개로 9000~9004 포트를 통째로 점유**하고 있어서 MinIO의 Docker 포트 매핑과 충돌한 것이었습니다. curl은 어느 프로세스가 응답하든 그럴듯한 응답이면 넘어가서 문제를 못 느꼈던 것. MinIO를 9100/9101로 옮겨서 해결했습니다 — 로컬에서 이 서비스를 실행할 때 9000번대 포트가 이미 쓰이고 있다면 먼저 의심할 것.
- 검증 완료(2026-08-25): 실제 PDF 업로드 → S3에 저장되고 DB에 메타데이터 기록 → `document.uploaded` 이벤트를 Kafka에서 직접 consume해 페이로드 확인 → 목록/상세 조회 → 다운로드한 파일이 원본과 바이트 단위로 동일함 확인 → 제목 수정 → 허용 안 되는 파일 형식(txt) 업로드 시 400 → 삭제 후 목록에서 사라지고 상세 조회 404, S3 오브젝트도 실제로 삭제됨, `document.deleted` 이벤트 발행 확인 → 일반 직원 계정으로 접근 시 403 — 전부 실제 인프라(MariaDB+Kafka+MinIO)에 대해 curl과 AWS CLI로 확인.
- 검증 완료(2026-08-26, 임직원 다운로드): 직원 계정으로 본인 회사 문서 다운로드 → 원본과 바이트 단위로 동일 확인, 존재하지 않는 id는 404, 미로그인 요청은 403, 문서 목록 등 다른 API는 여전히 403(다운로드만 예외로 열렸는지 확인) — 게이트웨이 경유로 확인. search-service가 색인용으로 쓰는 내부 다운로드(`download(id)`, companyId 확인 없음)는 그대로 잘 동작하는지도 재확인.

## search-service 참고

- 포트 8084. document-service가 발행하는 `document.uploaded`/`document.deleted`를 구독해 원문 다운로드 → 텍스트 추출 → Chunking → 임베딩 → Elasticsearch 색인까지 비동기로 처리하고, `/internal/search`로 하이브리드(벡터+키워드) 검색을 제공합니다. 자기 전용 DB는 없습니다 — 청크 텍스트와 벡터를 전부 ES 인덱스 문서 자체에 저장하는 구조라 별도 관계형 테이블이 필요 없습니다.
- **벡터 검색 스택 결정**: 발표자료엔 "Vector DB & ES"로 두 개의 박스로 그려져 있지만, 로컬 개발 편의와 인프라 단순화를 위해 **Elasticsearch 하나로 통합**했습니다. ES가 `dense_vector` 필드(kNN 벡터 검색)와 BM25(키워드 검색)를 동시에 지원해서, 별도 벡터 DB(Milvus/Qdrant 등) 없이 인덱스 하나에서 하이브리드 검색을 구현합니다.
- **임베딩 결정**: OpenAI API 키가 아직 없어서, 로컬 Docker 컨테이너로 띄운 **Ollama + nomic-embed-text**(768차원, 무료)로 임베딩을 생성합니다. `EmbeddingClient` 인터페이스 뒤에 `OllamaEmbeddingClient` 구현체 하나만 두는 구조라, 나중에 OpenAI API 키가 생기면 구현체를 하나 더 추가하고 활성 빈만 바꾸면 됩니다(Chunking·색인·검색 코드는 인터페이스만 참조). 다만 임베딩 모델을 바꾸면 벡터 차원이 달라질 수 있어 ES 인덱스도 새로 만들어야 합니다.
- **파이프라인**(`event/DocumentEventListener` → `index/DocumentChunkIndexer`): `document.uploaded` 수신 → `client/DocumentServiceClient`가 document-service의 내부 API(`X-Internal-Token`)로 원본 바이트를 받아옴 → `extract/TextExtractor`(Apache PDFBox)로 텍스트 추출 → `chunk/TextChunker`가 문단 단위로 묶어 800자 안팎(100자 겹침)으로 분할 → 청크마다 Ollama로 임베딩 생성 → ES에 Bulk 색인(문서 ID `{documentId}-{chunkIndex}`, 재색인 시 먼저 기존 청크를 지워 개수가 줄어도 오래된 청크가 안 남게 함). `document.deleted` 수신 시엔 해당 documentId의 청크를 전부 delete-by-query로 제거합니다.
- **HWP는 아직 지원하지 않습니다** — 자바 생태계에 성숙한 HWP 파서가 마땅치 않아서, PDF만 텍스트를 추출합니다. HWP 업로드 이벤트는 `UnsupportedFileFormatException`으로 걸러 로그만 남기고 색인을 건너뜁니다(document-service의 원본 저장 자체는 정상 — 검색 대상에서만 빠짐).
- **검색**: `HybridSearchService`가 BM25 검색과 kNN 벡터 검색을 각각 실행한 뒤 [Reciprocal Rank Fusion](https://www.elastic.co/guide/en/elasticsearch/reference/current/rrf.html)으로 순위를 합칩니다(ES의 `retriever.rrf` API 대신 직접 구현 — 이해·검증이 쉬워서 이 방식을 택함, 공식은 동일). `GET /internal/search?companyId=&query=&topK=`로 노출되며, chat-service가 RAG 답변을 만들 때 이 API를 호출합니다. companyId로 필터링해 테넌트 격리(REQ-F-018)를 지킵니다.
- **의존성**: `spring-boot-starter-elasticsearch`(co.elastic.clients 기반 `ElasticsearchClient` 자동 구성 — Spring Data의 리포지토리 추상화는 쓰지 않음, kNN+BM25를 섞은 커스텀 쿼리를 직접 짜야 해서 저수준 클라이언트가 더 맞음), `spring-boot-starter-kafka`, `org.apache.pdfbox:pdfbox`.
- Elasticsearch는 **Boot가 헬스 인디케이터를 기본 제공**합니다 (document-service의 Kafka와 달리 별도로 만들 필요 없었음). Kafka는 여전히 Boot 4.1에서 기본 제공되지 않아 `health/KafkaHealthIndicator.java`를 document-service와 동일하게 추가했습니다.
- **삽질 1 — ES 클라이언트/서버 버전 불일치**: Boot 4.1.1이 관리하는 `co.elastic.clients:elasticsearch-java`가 9.4.5로 고정되어 있는데, 처음 띄운 서버는 8.15.0이었습니다. 결국 서버도 9.4.5로 맞춰서 해결 — Boot 버전을 올릴 때마다 ES 클라이언트 버전이 따라 바뀔 수 있으니, 서버 이미지 태그도 같이 확인할 것.
- **삽질 2 — 색인은 되는데 `_source`에 `embedding` 필드가 안 보임**: Java 클라이언트(bulk/단건 모두)는 물론 순수 `curl`/Python `urllib`로 직접 색인해도 이 ES 9.4.5 이미지에서는 `dense_vector` 필드가 `_source` 조회에 항상 빠졌습니다(`index`/`_source.mode` 설정을 바꿔봐도 동일) — 클라이언트 버그가 아니라 서버 쪽 동작이었습니다. 원인을 더 파기 전에 실제로 필요한지부터 따져보니: 임베딩은 색인 시점에 벡터 인덱스 구조에 넣기 위해서만 필요하고, 검색 시점엔 매번 쿼리 텍스트를 새로 임베딩해서 비교하는 것이지 저장된 벡터를 다시 읽어올 일이 없었습니다. 즉 실제 기능에는 영향이 없는 곁가지였고, 색인/검색이 실제로 잘 동작하는지(dims 불일치 시 400 에러, kNN 검색 결과가 맞는지)로 검증 기준을 바꿔서 통과를 확인했습니다.
- 검증 완료(2026-08-26, 전부 실제 로컬 인프라로 확인): 관리자가 PDF 업로드 → Kafka `document.uploaded` 소비 → PDFBox 텍스트 추출 → 청크 생성 → Ollama 임베딩 → ES 색인까지 로그로 확인 → 영어 쿼리("How many days of annual leave...")로 검색 시 올바른 청크가 반환(BM25+kNN 둘 다 1위로 잡아 RRF 점수 최고) → **한국어 쿼리("연차는 몇일인가요")로도 같은 영문 문서를 찾아냄** — 텍스트에 겹치는 단어가 전혀 없어 BM25는 못 잡고 벡터 검색만으로 찾은 것으로, 진짜 의미 기반 검색이 되는지 확인한 포인트. 다른 회사(companyId) 것처럼 조회하면 빈 배열(테넌트 격리), `X-Internal-Token` 누락/오류 시 400/403, 문서 삭제 시 청크도 즉시 제거, HWP 업로드는 색인만 건너뛰고 서비스는 안 죽음 — 전부 확인.

## chat-service 참고

- 포트 8085. 자기 전용 DB(`gyubot_chat`)를 씀 — `chat_session`/`chat_message`/`answer_source` 3개 테이블(pptx 데이터모델의 "AI 대화 이력" 도메인 그대로). auth-service가 발급한 JWT를 검증만 하며, `/api/chat/**`는 임직원·관리자 구분 없이 로그인만 하면 됩니다(AI 질의는 두 역할 다 쓰는 기능).
- API: `POST /api/chat/messages`(질문 하나를 보내고 답변+근거를 받음 — `sessionId`가 없으면 새 대화방을 만듦) · `GET /api/chat/sessions`(질의 이력 목록) · `GET /api/chat/sessions/{id}/messages`(대화방 하나의 전체 메시지+근거).
- **LLM 결정**: 임베딩과 마찬가지로 OpenAI/Anthropic API 키가 아직 없어서, 로컬 Ollama + `qwen2.5:7b`(한국어 응답이 괜찮은 모델, 무료)로 답변을 생성합니다. `llm/ChatClient` 인터페이스 뒤에 `OllamaChatClient` 구현체 하나만 두는 구조라, 나중에 API 키가 생기면 search-service의 `EmbeddingClient`와 똑같은 방식으로 구현체만 하나 더 추가하면 됩니다.
- **RAG 흐름**(`ChatService.ask()`): 사용자 메시지 저장 → `client/SearchServiceClient`로 search-service의 `/internal/search`를 호출해 관련 청크 상위 5개를 가져옴 → 청크들을 "이 내용만 근거로 답하고, 없으면 모른다고 하라"는 시스템 프롬프트에 넣어 Ollama에 전달 → 답변을 어시스턴트 메시지로 저장 → 검색 결과 각각을 `answer_source`로 저장해 REQ-F-007(근거 표시)을 충족. 검색 결과가 아예 없으면 LLM을 호출하지 않고 고정 안내 문구를 반환합니다.
- **대화방 소유권 검증**: `sessionId`로 특정 대화방에 메시지를 보내거나 이력을 조회할 때, 그 대화방이 요청자 본인 것(companyId+userId 일치)인지 확인합니다. 다른 사람 것이면 403이 아니라 **404로 존재 자체를 숨깁니다** — 다른 회사/다른 직원의 대화방이 있는지 없는지조차 알려주지 않기 위함(테넌트 격리, REQ-F-018).
- 검증 완료(2026-08-26, 전부 실제 인프라로 확인): 직원 계정으로 "연차 휴가는 며칠까지 쓸 수 있고, 신청은 언제까지 해야 해?" 질문 → search-service에서 올바른 청크를 찾아 근거로 제시하며 정확한 한국어 답변 생성 → 같은 세션에서 "이월은 며칠까지 가능해?" 후속 질문도 문맥 없이(매 요청이 검색부터 새로 하는 구조라 이전 대화 맥락은 없음, 검색 자체가 매번 관련 규정을 다시 찾아줌) 정확히 답변 → 질의 이력 목록/상세 조회 확인 → 인증 없이 호출 시 403 → 다른 사용자(관리자)가 남의 대화방 조회 시 404 → 색인된 문서와 무관한 질문("화성 여행 경비는 얼마나 지원돼?")에는 근거 문서에 없다며 **지어내지 않고 답변을 거절** — 환각 방지가 실제로 동작하는 것까지 확인.

## 가입 승인 플로우 (예외 가입)

REQ-F-002·003 기준 — 회사 이메일이 없는 사용자가 명함·재직증명서(JPG/PNG/PDF)를 첨부해 가입을 신청하면, 관리자가 검토해 승인/반려합니다.

- `POST /api/users/signup-requests` (multipart, 인증 불필요) — 이메일/이름/비밀번호/첨부파일 접수. 비밀번호는 접수 시점에 즉시 BCrypt로 해시해 저장하고 원문은 어디에도 남기지 않습니다. 이미 가입된 이메일(409), 이미 대기 중인 신청(409), 허용되지 않는 파일 형식(400)은 여기서 막습니다.
- `GET /api/users/signup-requests` · `GET /api/users/signup-requests/{id}` · `GET /api/users/signup-requests/{id}/attachment` — 관리자용 대기 목록/상세/첨부파일 열람 (`FileStorageService`가 로컬 디스크에 저장, `app.upload.dir` 기본값 `/tmp/gyubot-uploads` — document-service의 S3 저장이 생기면 그쪽 호출로 교체 예정).
- `POST /api/users/signup-requests/{id}/approve` — auth-service의 `POST /internal/auth-users`를 호출해 실제 로그인 계정을 만들고(비밀번호는 재해시 없이 그대로 전달), 같은 id로 `member_profile`도 만든 뒤 승인 메일을 보냅니다.
- `POST /api/users/signup-requests/{id}/reject` — 사유와 함께 반려 처리 후 반려 메일 발송. 이미 처리된 신청을 다시 승인/반려하면 409.
- 데모 스코프 한계: 회사 선택 UI/테넌트 관리가 없어 모든 신청을 `company_id=1`로 고정 접수합니다 (실제로는 신청 시점에 회사를 지정하거나 관리자가 배정해야 함).
- 검증 완료(2026-08-25, curl + 실제 Chrome 브라우저 모두): 중복 이메일/중복 대기/잘못된 파일 형식 각각의 거부, 승인 시 auth-service 계정 생성과 새 비밀번호로 실제 로그인 성공, 승인/반려 메일이 Mailpit에 정확한 내용으로 도착, 이미 처리된 신청 재처리 시 409.
