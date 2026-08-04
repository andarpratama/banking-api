# banking-api

Enterprise Banking REST API (portfolio) — Java 21, Spring Boot, feature-first Clean Architecture, PostgreSQL, Redis.

## Docs

See [docs/README.md](docs/README.md) for BRD/PRD/SRS, ADR, OpenAPI, testing, security, and deployment guides (organized under `docs/` subfolders).

Docker files live under [`docker/`](docker/).

## Run locally (profile `dev`)

```bash
# Optional: Postgres + Redis for later infra tasks
docker compose -f docker/docker-compose.dev.yml up -d postgres redis

mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

API listens on **http://localhost:8080**. Base path `/api/v1` is set on controllers (not servlet context-path). Config: `application.yml` + `application-dev.yml` / `application-prod.yml` — datasource, Redis, and JWT via env vars (see [Development Setup](docs/engineering/Banking_API_Development_Setup.md)).

## Local agent tooling (not in remote)

These stay on your machine (gitignored): `.ai/`, `.cursor/`, `AGENTS.md`, `CLAUDE.md`.
