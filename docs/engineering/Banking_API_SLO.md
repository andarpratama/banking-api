# Service Level Objectives — Banking API

**Version:** 1.0.0  
**Date:** 2026-08-20  
**Status:** Local / portfolio contract (not a customer SLA)

These SLOs are the reliability targets for `/api/v1`. They sit on the T-093 Prometheus scrape + Grafana stack. **Error-budget remaining and MWMB burn-rate alerts** are in [Banking_API_Error_Budget.md](Banking_API_Error_Budget.md) (T-097). This document defines SLIs, targets, and how to read them.

Targets align with [Security & Performance](Banking_API_Security_Performance.md) §2.1 (p99 &lt; 300 ms, response max 500 ms) and the Wave 1 money-path note (deposit p99 under 200 ms).

---

## 1. What is in scope

| Term | Meaning here |
|------|----------------|
| **SLI** | Ratio or quantile computed from Micrometer `http.server.requests` |
| **SLO** | Target we want the SLI to meet over a window |
| **SLA** | Not used. This is an internal ops contract, not a customer guarantee |

**Included:** authenticated and public `/api/v1/**` application traffic (auth, customers, accounts, transactions, dashboard, audit).

**Excluded from the denominator (do not burn the SLO):**

- Client errors (`4xx`) — bad requests, auth failures, and validation are not server faults
- Health probes (`/api/v1/health`, `/live`, `/ready`)
- Actuator scrape (`/actuator/**`)
- Local chaos experiments (T-095) while Postgres/Redis are intentionally down

Local Prometheus keeps **24 h** of samples (`--storage.tsdb.retention.time=24h`). Gauges use a **5 m** rolling window; the documented production window is **30 days** (needs longer retention or remote write — not in this task).

---

## 2. SLO catalog

| ID | SLO | Target | SLI (5 m) | Local window | Prod-like window |
|----|-----|--------|-----------|--------------|------------------|
| **SLO-01** | Availability | **99.9%** | `banking:sli:availability:ratio5m` | 24 h (retention) | 30 d |
| **SLO-02** | Error rate | **&lt; 0.1%** 5xx | `banking:sli:error_rate:ratio5m` | 24 h | 30 d |
| **SLO-03** | Latency p99 | **&lt; 300 ms** | `banking:sli:latency_p99:seconds5m` | 24 h | 30 d |

SLO-01 and SLO-02 are two views of the same events: availability = 1 − error rate on **valid** requests (not 4xx).

**Stretch (money path, not a separate SLO id):** deposit / withdraw / transfer p99 **&lt; 200 ms** — `banking:sli:latency_p99_money:seconds5m`. Hard ceiling from the performance doc remains **500 ms** (page red if p99 exceeds 1 s).

```
┌─ SLI vs SLO (Grafana :3001 → Banking API / Service Level Objectives) ─┐
│  [avail % vs 99.9%]  [5xx % vs 0.1%]  [p99 vs 300 ms]                 │
│  timeseries with threshold lines  ·  money-path p99 vs 200 ms         │
└───────────────────────────────────────────────────────────────────────┘
```

---

## 3. SLI formulas

Selector shared by all rules:

```text
uri=~"/api/v1/.*"
uri!~"/api/v1/health.*"
status  — see each SLI
```

**Valid events** = `/api/v1` requests that are not 4xx and not health.  
**Good events** = those valid events with `2xx` or `3xx`.

### SLO-01 Availability

```promql
# recording rule: banking:sli:availability:ratio5m
sum(rate(http_server_requests_seconds_count{
    uri=~"/api/v1/.*", uri!~"/api/v1/health.*", status=~"[23].."
  }[5m]))
/
clamp_min(
  sum(rate(http_server_requests_seconds_count{
    uri=~"/api/v1/.*", uri!~"/api/v1/health.*", status!~"4.."
  }[5m])),
  1e-9
)
```

Target: **≥ 0.999**.

### SLO-02 Error rate

```promql
# recording rule: banking:sli:error_rate:ratio5m
sum(rate(http_server_requests_seconds_count{
    uri=~"/api/v1/.*", uri!~"/api/v1/health.*", status=~"5.."
  }[5m]))
/
clamp_min(
  sum(rate(http_server_requests_seconds_count{
    uri=~"/api/v1/.*", uri!~"/api/v1/health.*", status!~"4.."
  }[5m])),
  1e-9
)
```

Target: **&lt; 0.001**.

### SLO-03 Latency p99

Successful application requests only (`2xx`), so failed attempts do not inflate the quantile:

```promql
# recording rule: banking:sli:latency_p99:seconds5m
histogram_quantile(0.99,
  sum by (le) (
    rate(http_server_requests_seconds_bucket{
      uri=~"/api/v1/.*", uri!~"/api/v1/health.*", status=~"2.."
    }[5m])
  )
)
```

Target: **&lt; 0.3 s**. Money-path filter: `uri=~"/api/v1/transactions/(deposit|withdraw|transfer)"` vs **0.2 s**.

Recording rules live in `docker/observability/prometheus/rules/slo-recording-rules.yml` (loaded by `docker/observability/prometheus.yml`).

---

## 4. How to read the dashboard

```bash
docker compose -f docker/docker-compose.dev.yml up -d
mvn spring-boot:run -Dspring-boot.run.profiles=dev
curl -sf http://localhost:8080/actuator/prometheus | head
# Grafana: http://localhost:3001  →  Dashboards → Banking API → Service Level Objectives
# Prometheus rules: http://localhost:9090/rules
```

| Signal | Green | Investigate |
|--------|-------|-------------|
| Availability | ≥ 99.9% | Readiness 503, 5xx spike, or dependency outage ([chaos playbook](Banking_API_Chaos_Engineering_Playbook.md)) |
| Error rate | &lt; 0.1% | HTTP Error Rate dashboard, then Jaeger traces for 5xx spans |
| p99 | &lt; 300 ms | HTTP Latency dashboard + `trace_id` in JSON logs |

No traffic → gauges may be empty (`rate()` is zero). That is **not** an SLO miss; wait until the API is serving `/api/v1` requests.

This is **not** an SLA. Remaining budget and local burn-rate alerts are in [Banking_API_Error_Budget.md](Banking_API_Error_Budget.md) (T-097) — still not production paging.

---

## 5. Related

| Topic | Where |
|-------|--------|
| Grafana latency / 5xx (raw) | [Deployment Guide](Banking_API_Deployment_Guide.md) § Grafana dashboards |
| Performance maxima | [Security & Performance](Banking_API_Security_Performance.md) §2.1 |
| Local fault injection | [Chaos Engineering Playbook](Banking_API_Chaos_Engineering_Playbook.md) |
| Error budget | [Banking_API_Error_Budget.md](Banking_API_Error_Budget.md) (T-097) |
