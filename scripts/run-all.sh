#!/usr/bin/env bash
# GyuBot 로컬 전체 기동: infra(docker compose) -> 백엔드 7개 서비스(순차, 헬스체크 통과 후 다음) -> 프론트엔드
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs"
PID_DIR="$ROOT_DIR/.pids"
mkdir -p "$LOG_DIR" "$PID_DIR"

# name:port:health_path (discovery-service는 actuator가 없어 대시보드 루트로 확인)
SERVICES=(
  "discovery-service:8761:/"
  "api-gateway:8080:/actuator/health"
  "auth-service:8081:/actuator/health"
  "user-service:8082:/actuator/health"
  "document-service:8083:/actuator/health"
  "search-service:8084:/actuator/health"
  "chat-service:8085:/actuator/health"
)

wait_for_http() {
  local name="$1" url="$2" timeout="${3:-180}" waited=0
  echo "  -> $name 기동 대기 중 ($url)"
  until curl -sf "$url" >/dev/null 2>&1; do
    sleep 2
    waited=$((waited + 2))
    if [ "$waited" -ge "$timeout" ]; then
      echo "  !! $name 이(가) ${timeout}s 내에 응답하지 않습니다. logs/$name.log 를 확인하세요." >&2
      return 1
    fi
  done
  echo "  -> $name OK (${waited}s)"
}

echo "[0/3] 인프라(docker compose) 기동..."
(cd "$ROOT_DIR/infra" && docker compose up -d)

echo "  -> 컨테이너 healthcheck 대기 중 (최대 120초, mailpit처럼 healthcheck 없는 컨테이너는 건너뜀)..."
waited=0
while true; do
  unhealthy=$(cd "$ROOT_DIR/infra" && docker compose ps --format '{{.Name}} {{.Health}}' \
    | awk '$2!="healthy" && $2!="" {print $1}')
  if [ -z "$unhealthy" ]; then
    echo "  -> 인프라 준비 완료"
    break
  fi
  sleep 3
  waited=$((waited + 3))
  if [ "$waited" -ge 120 ]; then
    echo "  !! 아직 healthy 상태가 아닌 컨테이너: $unhealthy (일단 계속 진행합니다)" >&2
    break
  fi
done

echo "[1/3] 백엔드 서비스 순차 기동 (discovery-service부터)"
for entry in "${SERVICES[@]}"; do
  name="${entry%%:*}"
  rest="${entry#*:}"
  port="${rest%%:*}"
  path="${rest#*:}"

  echo "[$name] ./gradlew bootRun 시작 (:$port), 로그: logs/$name.log"
  (
    cd "$ROOT_DIR/backend/$name" || exit 1
    nohup ./gradlew bootRun > "$LOG_DIR/$name.log" 2>&1 &
    echo $! > "$PID_DIR/$name.pid"
  )

  if ! wait_for_http "$name" "http://localhost:$port$path" 180; then
    echo "!! $name 기동 실패로 중단합니다. 이미 띄운 서비스는 scripts/stop-all.sh 로 정리하세요." >&2
    exit 1
  fi
done

echo "[2/3] 프론트엔드 기동"
if [ ! -d "$ROOT_DIR/frontend/node_modules" ]; then
  echo "  -> node_modules 없음, npm install 실행 (최초 1회)"
  (cd "$ROOT_DIR/frontend" && npm install)
fi
(
  cd "$ROOT_DIR/frontend" || exit 1
  nohup npm run dev > "$LOG_DIR/frontend.log" 2>&1 &
  echo $! > "$PID_DIR/frontend.pid"
)
sleep 2

echo ""
echo "[3/3] 완료."
echo "  Eureka 대시보드: http://localhost:8761"
echo "  API Gateway:     http://localhost:8080"
echo "  Mailpit(메일):   http://localhost:8025"
echo "  Frontend:        logs/frontend.log 에서 실제 접속 주소 확인 (보통 http://localhost:5173)"
echo "  전체 로그:       $LOG_DIR/"
echo "  중지:            scripts/stop-all.sh"
