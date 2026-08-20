# Docker Setup - Banking API

## Quick Start

```bash
# Development (deps: Postgres, Redis, Jaeger, Prometheus, Grafana — run app locally with mvn)
docker-compose -f docker/docker-compose.dev.yml up -d

# Optional ELK (Elasticsearch + Logstash + Kibana). Extra ~2.5GB RAM.
docker-compose -f docker/docker-compose.dev.yml --profile elk up -d

# Optional chaos fault injector (localhost:8099). Local experiments only.
docker compose -f docker/docker-compose.dev.yml -f docker/docker-compose.chaos.yml --profile chaos up -d

# Full stack (API + PostgreSQL + Redis + pgAdmin)
docker-compose -f docker/docker-compose.yml up -d

# Production
docker-compose -f docker/docker-compose.prod.yml up -d
```

## Files Overview

- **Dockerfile** - Multi-stage build for production
- **Dockerfile.dev** - Development image with live reload
- **Dockerfile.prod** - Lightweight production image
- **docker-compose.yml** - Full stack (API + PostgreSQL + Redis + pgAdmin)
- **docker-compose.dev.yml** - Development deps (Postgres, Redis, pgAdmin, Jaeger, Prometheus, Grafana; optional ELK via `--profile elk`)
- **docker-compose.chaos.yml** - Overlay, profile `chaos`: notification HTTP fault injector (`:8099`)
- **docker-compose.prod.yml** - Production setup
- **chaos/** - Fault-injector script mounted by the chaos overlay
- **.dockerignore** - Files to exclude from build
- **observability/** - Prometheus scrape + SLO recording rules, Grafana dashboards, Logstash pipeline

## Building Images

```bash
# Build for development
docker build -t banking-api:dev -f docker/Dockerfile.dev .

# Build for production
docker build -t banking-api:latest -f docker/Dockerfile .

# Build multi-arch for production
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t banking-api:latest \
  -f docker/Dockerfile .
```

## Environment Variables

Create `.env` file (optional — defaults are sensible for dev):

```
POSTGRES_DB=banking_api
POSTGRES_USER=banking_user
POSTGRES_PASSWORD=SecurePassword123!
REDIS_PASSWORD=RedisPassword123!
JWT_SECRET=your-super-secret-jwt-key-min-32-characters
```

**Development with docker-compose.dev.yml:**
When running the app locally (e.g., `mvn spring-boot:run -Dspring-boot.run.profiles=dev`), the default values in `application-dev.yml` will match the Docker services:
- Postgres: `localhost:5432` (or set `SPRING_DATASOURCE_URL` env var)
- Redis: `localhost:6379` (or set `SPRING_DATA_REDIS_HOST/PORT` env vars)
- Jaeger UI: http://localhost:16686 (OTLP gRPC `localhost:4317`)
- Grafana dashboards: http://localhost:3001 (admin/admin) — HTTP latency, error rate, and SLOs
- Prometheus: http://localhost:9090 (scrapes `host.docker.internal:8080/actuator/prometheus`; SLO recording rules at `/rules`)
- Kibana (optional `--profile elk`): http://localhost:5601 — data view `banking-api-*`
- Chaos injector (optional `--profile chaos`): http://localhost:8099 — see [Chaos Engineering Playbook](../docs/engineering/Banking_API_Chaos_Engineering_Playbook.md)

## Health Checks

### Using docker-compose.dev.yml (deps only)
```bash
# Database
docker-compose -f docker/docker-compose.dev.yml exec postgres psql -U banking_user -d banking_api -c "SELECT 1;"

# Redis
docker-compose -f docker/docker-compose.dev.yml exec redis redis-cli ping

# ELK (only after --profile elk)
curl -sf http://localhost:9200/_cluster/health
curl -sf http://localhost:5601/api/status
```

### Using docker-compose.yml (full stack)
```bash
# API health
curl http://localhost:8080/api/v1/health

# Database
docker-compose exec postgres psql -U banking_user -d banking_api -c "SELECT 1;"

# Redis
docker-compose exec redis redis-cli ping
```

## Troubleshooting

### Port already in use
```bash
docker-compose down
# Kill the process or use different ports
```

### Database connection failed
```bash
docker-compose logs postgres
```

### Clear everything
```bash
docker-compose down -v
docker system prune -a
```

---

See also: [Deployment Guide](../docs/engineering/Banking_API_Deployment_Guide.md)
