#!/bin/sh
# T-101 hot standby: clone the primary via pg_basebackup, then start in recovery.
# Local Compose only. Image: postgres:17-alpine (gosu + pg_basebackup).
set -eu

PRIMARY_HOST="${PRIMARY_HOST:-postgres}"
PRIMARY_PORT="${PRIMARY_PORT:-5432}"
REPL_USER="${POSTGRES_REPLICATION_USER:-replicator}"
REPL_PASSWORD="${POSTGRES_REPLICATION_PASSWORD:-change-me-replication}"
APP_USER="${POSTGRES_USER:-banking_user}"
APP_PASSWORD="${POSTGRES_PASSWORD:-SecurePassword123!}"
APP_DB="${POSTGRES_DB:-banking_api}"
PGDATA="${PGDATA:-/var/lib/postgresql/data}"
SLOT="${POSTGRES_REPLICATION_SLOT:-region_b_slot}"

echo "T-101 replica: waiting for primary ${PRIMARY_HOST}:${PRIMARY_PORT}"
until pg_isready -h "${PRIMARY_HOST}" -p "${PRIMARY_PORT}" -U "${APP_USER}" >/dev/null 2>&1; do
  sleep 2
done

ensure_primary() {
  export PGPASSWORD="${APP_PASSWORD}"
  psql -h "${PRIMARY_HOST}" -p "${PRIMARY_PORT}" -U "${APP_USER}" -d "${APP_DB}" -v ON_ERROR_STOP=1 <<EOSQL
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${REPL_USER}') THEN
    EXECUTE format('CREATE ROLE %I WITH REPLICATION LOGIN PASSWORD %L', '${REPL_USER}', '${REPL_PASSWORD}');
  ELSE
    EXECUTE format('ALTER ROLE %I WITH REPLICATION LOGIN PASSWORD %L', '${REPL_USER}', '${REPL_PASSWORD}');
  END IF;
END
\$\$;

SELECT pg_create_physical_replication_slot('${SLOT}')
WHERE NOT EXISTS (SELECT 1 FROM pg_replication_slots WHERE slot_name = '${SLOT}');
EOSQL
  unset PGPASSWORD
}

ensure_primary

mkdir -p "${PGDATA}"
chown -R postgres:postgres "${PGDATA}" || true

if [ ! -f "${PGDATA}/standby.signal" ]; then
  echo "T-101 replica: cloning primary (slot=${SLOT})"
  find "${PGDATA}" -mindepth 1 -delete 2>/dev/null || true
  export PGPASSWORD="${REPL_PASSWORD}"
  attempt=0
  while [ "${attempt}" -lt 15 ]; do
    if gosu postgres pg_basebackup \
      -h "${PRIMARY_HOST}" \
      -p "${PRIMARY_PORT}" \
      -U "${REPL_USER}" \
      -D "${PGDATA}" \
      -Fp -Xs -P -R \
      --slot="${SLOT}"; then
      unset PGPASSWORD
      chown -R postgres:postgres "${PGDATA}"
      break
    fi
    attempt=$((attempt + 1))
    echo "T-101 replica: pg_basebackup retry ${attempt}/15"
    sleep 2
  done
  unset PGPASSWORD
  if [ ! -f "${PGDATA}/standby.signal" ]; then
    echo "T-101 replica: pg_basebackup failed (is the replication overlay applied on the primary?)" >&2
    exit 1
  fi
fi

exec docker-entrypoint.sh postgres -c hot_standby=on -c hot_standby_feedback=on
