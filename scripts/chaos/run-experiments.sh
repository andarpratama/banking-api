#!/usr/bin/env bash
# T-095 local chaos runner. Requires the API on $API_BASE_URL and Compose deps.
# Always restarts postgres/redis on exit. Not for production.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE=(docker compose -f "${ROOT}/docker/docker-compose.dev.yml")
CHAOS_COMPOSE=(
  docker compose
  -f "${ROOT}/docker/docker-compose.dev.yml"
  -f "${ROOT}/docker/docker-compose.chaos.yml"
  --profile chaos
)
API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
LIVE="${API_BASE_URL}/api/v1/health/live"
READY="${API_BASE_URL}/api/v1/health/ready"
FAULT_URL="${FAULT_BASE_URL:-http://localhost:8099}"
WAIT_SECS="${CHAOS_WAIT_SECS:-45}"

usage() {
  cat <<'EOF'
Usage: ./scripts/chaos/run-experiments.sh [EXP-01|EXP-02|EXP-03|EXP-04|all]

Local Docker Compose only. Abort if liveness leaves 200 during EXP-01/02.

  API_BASE_URL     default http://localhost:8080
  CHAOS_WAIT_SECS  poll timeout (default 45)
EOF
}

http_code() {
  local code
  code="$(curl -sS -o /dev/null -w '%{http_code}' --max-time 5 "$1" 2>/dev/null || true)"
  if [[ -z "${code}" ]]; then
    echo "000"
  else
    echo "${code}"
  fi
}

wait_for_code() {
  local url="$1"
  local expected="$2"
  local label="$3"
  local elapsed=0
  local code
  while [[ "${elapsed}" -lt "${WAIT_SECS}" ]]; do
    code="$(http_code "${url}")"
    if [[ "${code}" == "${expected}" ]]; then
      echo "  ${label}: HTTP ${code}"
      return 0
    fi
    sleep 1
    elapsed=$((elapsed + 1))
  done
  echo "FAIL ${label}: expected ${expected}, last=${code:-000}" >&2
  return 1
}

assert_liveness_up() {
  local code
  code="$(http_code "${LIVE}")"
  if [[ "${code}" != "200" ]]; then
    echo "ABORT: liveness left 200 (got ${code}). Restore deps and stop." >&2
    exit 2
  fi
}

restore_deps() {
  "${COMPOSE[@]}" start postgres redis >/dev/null 2>&1 || true
}

cleanup() {
  restore_deps
}
trap cleanup EXIT

require_api() {
  local live
  live="$(http_code "${LIVE}")"
  if [[ "${live}" != "200" ]]; then
    echo "API liveness is ${live}. Start the app (mvn spring-boot:run -Dspring-boot.run.profiles=dev) first." >&2
    exit 1
  fi
}

exp_01() {
  echo "== EXP-01 PostgreSQL down =="
  echo "Hypothesis: live=200, ready=503 (database DOWN)."
  "${COMPOSE[@]}" stop postgres
  wait_for_code "${READY}" "503" "readiness after postgres stop"
  assert_liveness_up
  echo "PASS EXP-01"
  "${COMPOSE[@]}" start postgres
  wait_for_code "${READY}" "200" "readiness after postgres start"
}

exp_02() {
  echo "== EXP-02 Redis down =="
  echo "Hypothesis: live=200, ready=503 (cache DOWN)."
  "${COMPOSE[@]}" stop redis
  wait_for_code "${READY}" "503" "readiness after redis stop"
  assert_liveness_up
  echo "PASS EXP-02"
  "${COMPOSE[@]}" start redis
  wait_for_code "${READY}" "200" "readiness after redis start"
}

exp_03() {
  echo "== EXP-03 Notification vendor fault =="
  echo "Hypothesis: POST to injector returns 5xx; OPEN circuit must not fail readiness."
  NOTIFICATION_FAULT_MODE=fail "${CHAOS_COMPOSE[@]}" up -d --force-recreate notification-fault
  local ready_fault=0
  local elapsed=0
  while [[ "${elapsed}" -lt "${WAIT_SECS}" ]]; do
    ready_fault="$(http_code "${FAULT_URL}/health")"
    if [[ "${ready_fault}" == "200" ]]; then
      break
    fi
    sleep 1
    elapsed=$((elapsed + 1))
  done
  if [[ "${ready_fault}" != "200" ]]; then
    echo "FAIL notification-fault /health last=${ready_fault}" >&2
    return 1
  fi
  local post
  post="$(curl -sS -o /dev/null -w '%{http_code}' --max-time 5 \
    -X POST "${FAULT_URL}/v1/notifications" \
    -H 'Content-Type: application/json' \
    -d '{"event":"injected"}' 2>/dev/null || echo "000")"
  if [[ "${post}" != "500" ]]; then
    echo "FAIL injector POST expected 500, got ${post}" >&2
    return 1
  fi
  assert_liveness_up
  local ready
  ready="$(http_code "${READY}")"
  if [[ "${ready}" != "200" ]]; then
    echo "FAIL readiness must stay 200 while only the vendor is down (got ${ready})" >&2
    return 1
  fi
  echo "PASS EXP-03 injector=500, readiness still 200"
  echo "Manual: restart the API with NOTIFICATION_PROVIDER=http NOTIFICATION_HTTP_BASE_URL=${FAULT_URL}"
  echo "        then run a transfer — it must succeed (T-094 logging fallback)."
}

exp_04() {
  echo "== EXP-04 Restore dependencies =="
  echo "Hypothesis: after postgres+redis are up, ready=200 and live=200."
  restore_deps
  wait_for_code "${READY}" "200" "readiness after restore"
  assert_liveness_up
  echo "PASS EXP-04"
}

main() {
  local target="${1:-all}"
  if [[ "${target}" == "-h" || "${target}" == "--help" ]]; then
    usage
    trap - EXIT
    exit 0
  fi
  require_api
  wait_for_code "${READY}" "200" "baseline readiness"
  case "${target}" in
    all)
      exp_01
      exp_02
      exp_03
      exp_04
      ;;
    EXP-01) exp_01 ;;
    EXP-02) exp_02 ;;
    EXP-03) exp_03 ;;
    EXP-04) exp_04 ;;
    *)
      usage
      exit 1
      ;;
  esac
  echo "All requested experiments passed."
}

main "$@"
