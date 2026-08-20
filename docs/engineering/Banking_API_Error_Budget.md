# Error Budget Tracking — Banking API

**Version:** 1.0.0  
**Date:** 2026-08-20  
**Status:** Local / Google SRE model (not a customer SLA, not production paging)

Error budget is how much unreliability the [SLO catalog](Banking_API_SLO.md) still allows. This document tracks **remaining budget** and **burn rate** on the T-096 SLIs, and fires **multi-window multi-burn-rate (MWMB)** alerts locally.

Targets sit on the T-093 Prometheus scrape + Grafana stack. Alertmanager is **UI-only** — no PagerDuty, email, or webhook paging.

---

## 1. Google SRE model (this repo)

| Term | Meaning here |
|------|----------------|
| **Error budget** | `1 − SLO`. SLO-01/02 at 99.9% → **0.1%** of valid requests may be 5xx. SLO-03 p99 &lt; 300 ms → **1%** of successful requests may be slower than 300 ms |
| **Burn rate** | `error_ratio / error_budget`. **1×** exhausts the window exactly; **14.4×** would consume ~2% of a 30-day budget in 1 hour |
| **Remaining** | `1 − (error_ratio_window / error_budget)`, clamped at 0 |
| **MWMB** | Alert only when a **long** window **and** a **short** window both exceed the same burn factor (SRE workbook ch. 5) |

**Included / excluded events** match the SLO document (no 4xx, no health/actuator, no intentional T-095 chaos as “prod” truth).

Local Prometheus keeps **24 h** of samples. Remaining-budget gauges use a **24 h** window. The documented production window is **30 days** (needs longer retention or remote write — not in this task).

```
┌─ Error budget (Grafana :3001 → Banking API / Error Budget) ─┐
│  [avail remaining]  [burn 5m]  [hours to exhaust]           │
│  burn vs 1× / 6× / 14.4×  ·  latency remaining (p99 300 ms) │
└──────────────────────────────────────────────────────────────┘
Alertmanager UI: http://localhost:9093  (local-dev receiver — no paging)
Prometheus alerts: http://localhost:9090/alerts
```

---

## 2. Formulas

### Availability (SLO-01 / SLO-02)

```promql
# recording rule: banking:error_budget:availability:burn5m
banking:sli:error_rate:ratio5m / 0.001

# recording rule: banking:error_budget:availability:remaining24h
clamp_min(1 - banking:sli:error_rate:ratio24h / 0.001, 0)
```

Hours to exhaustion at the current 5 m burn (24 h local window):

```promql
banking:error_budget:availability:remaining24h * 24
  / clamp_min(banking:error_budget:availability:burn5m, 1e-9)
```

### Latency (SLO-03)

Successful `/api/v1` requests slower than **300 ms** vs a **1%** budget (`le="0.3"` histogram bucket from `management.metrics.distribution.slo`):

```promql
# recording rule: banking:sli:latency_slow:ratio5m
(ok_rate5m - fast300_rate5m) / clamp_min(ok_rate5m, 1e-9)

# recording rule: banking:error_budget:latency:burn5m
banking:sli:latency_slow:ratio5m / 0.01
```

Recording rules: `docker/observability/prometheus/rules/error-budget-recording-rules.yml`.  
Alert rules: `docker/observability/prometheus/rules/error-budget-alerts.yml` (T-096 `slo-recording-rules.yml` stays alert-free).

---

## 3. MWMB alerts (local)

| Alert | Long | Short | Factor | Severity | 30-day meaning |
|-------|------|-------|--------|----------|----------------|
| `BankingApiErrorBudgetFastBurn` | 1 h | 5 m | **14.4×** | page | ~2% of monthly budget in 1 h |
| `BankingApiErrorBudgetSlowBurn` | 6 h | 30 m | **6×** | page | ~5% of monthly budget in 6 h |
| `BankingApiErrorBudgetTicket` | 6 h | 1 h | **1×** | ticket | Local stand-in for 3 d / 6 h 1× (24 h TSDB) |
| `BankingApiLatencyBudgetFastBurn` | 1 h | 5 m | **6×** | ticket | Latency budget (1%) spending fast |

Production should add the workbook ticket pair **3 d + 6 h at 1×** (and optionally **1 d + 2 h at 3×**) once retention is ≥ 30 days.

Both windows must fire so a blip that already recovered does not page. `severity=page` here means “would page in prod” — Alertmanager’s `local-dev` receiver sends **nothing**.

---

## 4. Policy when the budget is exhausted

Operator policy only (no deploy freeze automation in this task):

| Remaining (24 h) | What to do |
|------------------|------------|
| **&gt; 50%** | Ship features; accept normal risk |
| **20–50%** | Slow down risky changes; watch burn |
| **0%** | Reliability work first; postmortem the burn; do not treat 5xx as “expected” |

This is **not** an SLA and **not** wired to paging.

---

## 5. How to read locally

```bash
docker compose -f docker/docker-compose.dev.yml up -d
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# Grafana:        http://localhost:3001  →  Dashboards → Banking API → Error Budget
# Prometheus:     http://localhost:9090/alerts  and  /rules
# Alertmanager:   http://localhost:9093
```

| Signal | Green | Investigate |
|--------|-------|-------------|
| Remaining availability | ≥ 50% | 5xx spike, readiness 503, dependency outage ([chaos playbook](Banking_API_Chaos_Engineering_Playbook.md)) |
| Burn 5 m | &lt; 1× | Fast burn 14.4× → treat as page-class; correlate Jaeger + HTTP Error Rate |
| Latency remaining | ≥ 50% | p99 vs 300 ms on `banking-slo`; money-path stretch is 200 ms |

No traffic → rates are ~0 and remaining looks full. That is **not** a recovered incident; wait until `/api/v1` is serving.

---

## 6. Related

| Topic | Where |
|-------|--------|
| SLI / SLO targets | [Banking_API_SLO.md](Banking_API_SLO.md) |
| Grafana latency / 5xx (raw) | [Deployment Guide](Banking_API_Deployment_Guide.md) § Grafana dashboards |
| Local fault injection | [Chaos Engineering Playbook](Banking_API_Chaos_Engineering_Playbook.md) |
