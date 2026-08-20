# Chaos Engineering Playbook — Banking API

**Audience:** operators running the local Docker Compose stack  
**Scope:** development / laptop only. Do **not** run these experiments against staging or production.  
**Related:** [Deployment Guide §6.6](Banking_API_Deployment_Guide.md) (notification circuit breaker), health probes `GET /api/v1/health/live` and `GET /api/v1/health/ready`.

---

## 1. Principles

| Rule | Meaning here |
|------|----------------|
| Hypothesis first | Each experiment states what must stay up and what may fail. |
| Small blast radius | Only Compose services `postgres`, `redis`, and `notification-fault`. Never the host JVM kill unless you intend EXP abort. |
| Steady state | Baseline: liveness **200**, readiness **200** (Postgres + Redis up). |
| Abort | If **liveness** leaves 200 while only a dependency is down, stop and restore. The process is unhealthy; probes are wrong. |
| Restore | Always bring Postgres and Redis back. The runner uses an `EXIT` trap. |

This playbook does **not** install Chaos Mesh, Gremlin, or a production traffic generator.

---

## 2. Steady state & signals

| Probe | Path | Healthy | Unhealthy |
|-------|------|---------|-----------|
| Liveness | `GET /api/v1/health/live` | 200 `{"status":"UP"}` | Process down (connection refused) |
| Readiness | `GET /api/v1/health/ready` | 200 `database=UP`, `cache=UP` | **503** if Postgres or Redis is down |
| Notification CB | Prometheus `resilience4j_circuitbreaker_*` | OPEN must **not** change readiness | Vendor HTTP is optional (`NOTIFICATION_PROVIDER=log` by default) |

Grafana (latency / 5xx): http://localhost:3001 · Jaeger: http://localhost:16686 · Prometheus: http://localhost:9090

---

## 3. Prerequisites

```bash
docker compose -f docker/docker-compose.dev.yml up -d
mvn spring-boot:run -Dspring-boot.run.profiles=dev
curl -sf http://localhost:8080/api/v1/health/live
curl -sf http://localhost:8080/api/v1/health/ready
```

Optional vendor injector (EXP-03):

```bash
docker compose -f docker/docker-compose.dev.yml -f docker/docker-compose.chaos.yml --profile chaos up -d
```

---

## 4. Experiments

### EXP-01 — PostgreSQL down

**Hypothesis:** The JVM stays alive (liveness 200). Readiness returns **503** with `database=DOWN`. Clients must be taken out of rotation (K8s would stop sending traffic).

**Blast radius:** Compose service `postgres` only.

**Method:**

```bash
./scripts/chaos/run-experiments.sh EXP-01
# or:
docker compose -f docker/docker-compose.dev.yml stop postgres
curl -sS -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/v1/health/live   # 200
curl -sS -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/v1/health/ready  # 503
```

**Expect:** live=200, ready=503, `database=DOWN`. Money endpoints fail (no system of record).

**Abort:** liveness ≠ 200.

**Restore:** `docker compose -f docker/docker-compose.dev.yml start postgres` then wait until ready=200.

---

### EXP-02 — Redis down

**Hypothesis:** Liveness stays 200. Readiness **503** with `cache=DOWN`. Auth/session/rate-limit that need Redis degrade; the process is not restarted.

**Blast radius:** Compose service `redis` only.

**Method:** `./scripts/chaos/run-experiments.sh EXP-02`

**Expect:** live=200, ready=503, `cache=DOWN`.

**Abort:** liveness ≠ 200.

**Restore:** start `redis`, wait for ready=200.

---

### EXP-03 — Notification vendor 5xx / timeout

**Hypothesis:** A down vendor must **not** fail transfers. Resilience4j instance `notification` opens after enough 5xx/timeouts; fallback is the logging stub (T-094). Readiness stays **200** (`management.health.circuitbreakers.enabled: false`).

**Blast radius:** container `notification-fault` on `localhost:8099`. Default mode `fail` (POST → 500). `NOTIFICATION_FAULT_MODE=hang` sleeps to trip the HTTP read timeout.

**Method (injector only — automated):**

```bash
./scripts/chaos/run-experiments.sh EXP-03
```

**Method (money path — manual):** restart the API with:

```bash
NOTIFICATION_PROVIDER=http
NOTIFICATION_HTTP_BASE_URL=http://localhost:8099
```

Login as `customer@banking.local` / `SecurePass123!`, then deposit/transfer as usual. The HTTP call may fail; the use case must still return success. Logs show fallback; Prometheus `resilience4j_circuitbreaker_state` for `notification` may be OPEN.

**Expect:** injector POST `/v1/notifications` = 500 (mode `fail`); readiness still 200; transfer still succeeds when `provider=http`.

**Abort:** readiness 503 when only the vendor is down (circuit breaker leaked into health).

**Restore:** `docker compose -f docker/docker-compose.dev.yml -f docker/docker-compose.chaos.yml --profile chaos stop notification-fault`. Set `NOTIFICATION_PROVIDER=log` again if you switched.

---

### EXP-04 — Restore dependencies

**Hypothesis:** After Postgres and Redis are running again, readiness returns **200** without restarting the API process.

**Method:** `./scripts/chaos/run-experiments.sh EXP-04`

**Expect:** live=200, ready=200, `database=UP`, `cache=UP`.

**Abort:** ready stays 503 for more than ~45s after `start` (check Compose healthchecks / JDBC pool).

---

## 5. Runner

```bash
./scripts/chaos/run-experiments.sh          # EXP-01 … EXP-04
./scripts/chaos/run-experiments.sh EXP-01
```

The script **always** `start`s `postgres` and `redis` on exit. It refuses to run if liveness is not already 200.

| Variable | Default | Purpose |
|----------|---------|---------|
| `API_BASE_URL` | `http://localhost:8080` | API under test |
| `CHAOS_WAIT_SECS` | `45` | Poll timeout for 200/503 |
| `NOTIFICATION_FAULT_MODE` | `fail` | Injector: `fail` / `hang` / `ok` |

---

## 6. Observability during a run

1. Readiness 503 → Grafana HTTP error-rate dashboard may tick up if clients keep calling money APIs.
2. Vendor failures → Jaeger spans on notification HTTP; logs `Notification primary failed; using fallback`.
3. `curl -sf http://localhost:8080/actuator/prometheus | grep resilience4j_circuitbreaker`

---

## 7. Out of scope (later waves)

- Kubernetes Chaos Mesh / pod kill (needs T-100 manifests)
- Multi-region partition / failover (T-102). Local WAL streaming: [PostgreSQL replication](Banking_API_Postgres_Replication.md) (T-101)
- Production game days
