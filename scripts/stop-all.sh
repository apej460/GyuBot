#!/usr/bin/env bash
# GyuBot 로컬 프로세스 전체 중지 (run-all.sh로 띄운 백엔드 7개 + 프론트엔드)
# 인프라(docker compose) 컨테이너는 기본적으로 그대로 둠 (재기동/모델 재다운로드 비용 때문).
# 인프라까지 같이 내리려면: scripts/stop-all.sh --infra
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_DIR="$ROOT_DIR/.pids"

BACKEND_PORTS=(8761 8080 8081 8082 8083 8084 8085)

echo "[1/2] 백엔드 서비스 중지 (포트 기준: ${BACKEND_PORTS[*]})"
for port in "${BACKEND_PORTS[@]}"; do
  pids=$(lsof -ti tcp:"$port" 2>/dev/null || true)
  if [ -n "$pids" ]; then
    echo "  -> :$port (pid $pids) 종료"
    kill $pids 2>/dev/null || true
  fi
done

echo "[2/2] 프론트엔드 중지"
if [ -f "$PID_DIR/frontend.pid" ]; then
  fpid="$(cat "$PID_DIR/frontend.pid")"
  kill "$fpid" 2>/dev/null || true
fi
# npm run dev가 자식으로 띄운 vite 프로세스까지 정리 (pid 파일만으로는 안 잡히는 경우 대비)
pkill -f "GyuBot/frontend.*vite" 2>/dev/null || true

rm -f "$PID_DIR"/*.pid

if [ "${1:-}" = "--infra" ]; then
  echo "인프라(docker compose)도 함께 종료합니다..."
  (cd "$ROOT_DIR/infra" && docker compose down)
  echo "완료 (인프라 포함 전체 종료)."
else
  echo "완료. 인프라 컨테이너는 계속 실행 중입니다 (같이 내리려면: scripts/stop-all.sh --infra)."
fi
