#!/bin/sh
# Runs only on first initdb of the primary (empty volume).
# Replica entrypoint also ensures role + slot so existing volumes work.
set -eu

REPL_USER="${POSTGRES_REPLICATION_USER:-replicator}"
REPL_PASSWORD="${POSTGRES_REPLICATION_PASSWORD:-change-me-replication}"
SLOT="${POSTGRES_REPLICATION_SLOT:-region_b_slot}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<EOSQL
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${REPL_USER}') THEN
    EXECUTE format('CREATE ROLE %I WITH REPLICATION LOGIN PASSWORD %L', '${REPL_USER}', '${REPL_PASSWORD}');
  END IF;
END
\$\$;

SELECT pg_create_physical_replication_slot('${SLOT}')
WHERE NOT EXISTS (SELECT 1 FROM pg_replication_slots WHERE slot_name = '${SLOT}');
EOSQL
