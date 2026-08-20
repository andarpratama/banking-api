#!/usr/bin/env bash
# T-101: assert local primary → replica streaming replication.
# Local Docker Compose only. App datasource stays on :5432.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE=(
  docker compose
  -f "${ROOT}/docker/docker-compose.dev.yml"
  -f "${ROOT}/docker/docker-compose.replication.yml"
  --profile replication
)
WAIT_SECS="${REPLICATION_WAIT_SECS:-90}"
PRIMARY_USER="${POSTGRES_USER:-banking_user}"
PRIMARY_DB="${POSTGRES_DB:-banking_api}"

usage() {
  cat <<'EOF'
Usage: ./scripts/postgres/verify-replication.sh

Starts postgres + postgres-replica (replication profile) if needed, then checks:
  - replica is in recovery (hot standby)
  - insert on primary is visible on replica
  - writes on replica are rejected
  - pg_stat_replication shows the region-b slot

  REPLICATION_WAIT_SECS  poll timeout (default 90)

Local Docker Compose only. Not a failover test (T-102). Not production.
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

psql_primary() {
  "${COMPOSE[@]}" exec -T postgres psql -U "${PRIMARY_USER}" -d "${PRIMARY_DB}" -v ON_ERROR_STOP=1 "$@"
}

psql_replica() {
  "${COMPOSE[@]}" exec -T postgres-replica psql -U "${PRIMARY_USER}" -d "${PRIMARY_DB}" -v ON_ERROR_STOP=1 "$@"
}

wait_healthy() {
  local elapsed=0
  echo "Waiting for postgres and postgres-replica (${WAIT_SECS}s)..."
  while [[ "${elapsed}" -lt "${WAIT_SECS}" ]]; do
    if "${COMPOSE[@]}" exec -T postgres pg_isready -U "${PRIMARY_USER}" -d "${PRIMARY_DB}" >/dev/null 2>&1 \
      && "${COMPOSE[@]}" exec -T postgres-replica pg_isready -U "${PRIMARY_USER}" -d "${PRIMARY_DB}" >/dev/null 2>&1; then
      echo "  both instances accept connections"
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  echo "FAIL: replica did not become ready in ${WAIT_SECS}s" >&2
  "${COMPOSE[@]}" ps postgres postgres-replica >&2 || true
  "${COMPOSE[@]}" logs --tail=80 postgres-replica >&2 || true
  return 1
}

echo "T-101 verify: bringing up primary + replica"
"${COMPOSE[@]}" up -d postgres postgres-replica
wait_healthy

in_recovery="$(psql_replica -Atc "SELECT pg_is_in_recovery();")"
if [[ "${in_recovery}" != "t" ]]; then
  echo "FAIL: replica pg_is_in_recovery()=${in_recovery} (expected t)" >&2
  exit 1
fi
echo "  replica is in recovery (hot standby)"

marker="t101-$(date +%s)-$$"
psql_primary -c "CREATE TABLE IF NOT EXISTS t101_replication_probe (id text PRIMARY KEY, created_at timestamptz NOT NULL DEFAULT now());"
psql_primary -c "INSERT INTO t101_replication_probe (id) VALUES ('${marker}');"

visible=""
elapsed=0
while [[ "${elapsed}" -lt 20 ]]; do
  visible="$(psql_replica -Atc "SELECT COUNT(*) FROM t101_replication_probe WHERE id = '${marker}';" 2>/dev/null || echo 0)"
  if [[ "${visible}" == "1" ]]; then
    break
  fi
  sleep 1
  elapsed=$((elapsed + 1))
done
if [[ "${visible}" != "1" ]]; then
  echo "FAIL: row ${marker} not visible on replica after ${elapsed}s" >&2
  exit 1
fi
echo "  insert on primary replicated to replica"

set +e
write_err="$(psql_replica -c "INSERT INTO t101_replication_probe (id) VALUES ('should-fail');" 2>&1)"
write_rc=$?
set -e
if [[ "${write_rc}" -eq 0 ]]; then
  echo "FAIL: replica accepted a write (expected read-only)" >&2
  exit 1
fi
echo "  replica rejected writes (read-only standby)"

lag="$(psql_primary -Atc "SELECT COALESCE(client_addr::text, 'unknown') || ' replay_lag=' || COALESCE(replay_lag::text, '0') FROM pg_stat_replication LIMIT 1;")"
if [[ -z "${lag}" ]]; then
  echo "FAIL: pg_stat_replication is empty (WAL receiver not connected)" >&2
  psql_primary -c "SELECT * FROM pg_stat_replication;" >&2
  exit 1
fi
echo "  ${lag}"

slot_active="$(psql_primary -Atc "SELECT active FROM pg_replication_slots WHERE slot_name = 'region_b_slot';")"
if [[ "${slot_active}" != "t" ]]; then
  echo "FAIL: pg_replication_slots.region_b_slot active=${slot_active:-missing}" >&2
  psql_primary -c "SELECT slot_name, slot_type, active FROM pg_replication_slots;" >&2
  exit 1
fi
echo "  slot region_b_slot is active"

psql_primary -c "DROP TABLE IF EXISTS t101_replication_probe;" >/dev/null

echo "OK T-101: async streaming replication is working (app still uses localhost:5432)."
