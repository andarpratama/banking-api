# Banking API

Enterprise-style **Banking REST API** (portfolio / learning project): a modular monolith built with **Java 21**, **Spring Boot 3**, and **feature-first Clean Architecture**.

It demonstrates production-oriented backend practices — JWT auth & RBAC, PostgreSQL + Flyway, Redis, OpenAPI, Docker, and Testcontainers — without a frontend or microservices split in v1.

## Why this project

- Portfolio backend that looks and feels like a real banking API (auth, customers, accounts, money paths planned)
- Clear layering: controllers stay thin; domain stays free of Spring/JPA
- Contract-first API via OpenAPI; docs live under [`docs/`](docs/)

## Stack

| Layer | Choice |
|-------|--------|
| Language / runtime | Java 21, Spring Boot 3.x |
| API | REST JSON, base path `/api/v1` |
| Docs | springdoc OpenAPI + Swagger UI |
| Database | PostgreSQL 17 + Flyway |
| Cache / token store | Redis |
| Auth | JWT access + refresh rotation, BCrypt, ADMIN / CUSTOMER RBAC |
| Test | JUnit 5, Mockito, AssertJ, Testcontainers |
| Ops | Docker Compose (`docker/`) |

## Current status (honest)

**Implemented**

- Health check, common error envelope, OpenAPI/Swagger
- Docker Compose (Postgres + Redis), Flyway baseline, JPA + Redis config
- Auth: register, login, refresh, logout
- RBAC (ADMIN / CUSTOMER) + ownership checks
- Customer CRUD / search / pagination
- Account: create, get/balance, freeze, close
- Customer/Account ownership integration tests (Testcontainers)

**Planned / in progress**

- Account unfreeze, deposit / withdraw / transfer, history & statements
- Audit logging, dashboard analytics, notifications
- CI, coverage gates, security hardening, deploy smoke

Product & design specs (including future modules) are already in [`docs/`](docs/README.md).

## Architecture (short)

```
Presentation → Application → Domain
Infrastructure implements Domain / Application ports
```

Features are packaged by capability (`auth`, `customer`, `account`, …), each with `presentation` / `application` / `domain` / `infrastructure`. See [ADR-0001](docs/architecture/ADR-0001-Architecture-Style.md).

## Quick start

**Prerequisites:** Java 21, Maven 3.9+, Docker.

```bash
git clone https://github.com/andarpratama/banking-api.git
cd banking-api

# Optional local env (copy and edit — never commit real secrets)
cp .env.example .env

# Dependencies
docker compose -f docker/docker-compose.dev.yml up -d postgres redis

# Run (dev profile: Flyway + seed users)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

| Check | URL / command |
|-------|----------------|
| Health | `curl http://localhost:8080/api/v1/health` |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

API base: **http://localhost:8080/api/v1**

### Dev seed users (`dev` profile only)

| Email | Password | Role |
|-------|----------|------|
| `admin@banking.local` | `SecurePass123!` | ADMIN |
| `customer@banking.local` | `SecurePass123!` | CUSTOMER |

Example login:

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@banking.local","password":"SecurePass123!"}'
```

More detail: [Development Setup](docs/engineering/Banking_API_Development_Setup.md) · [Docker](docker/).

## Documentation

| Area | Link |
|------|------|
| Docs index | [docs/README.md](docs/README.md) |
| Requirements (BRD / PRD / SRS) | [docs/requirements/](docs/requirements/) |
| Architecture | [docs/architecture/](docs/architecture/) |
| API contract | [docs/api/Banking_API_OpenAPI_Specification.md](docs/api/Banking_API_OpenAPI_Specification.md) |
| Database design | [docs/database/](docs/database/) |
| Testing / security / deploy | [docs/engineering/](docs/engineering/) |

## Project layout

```
src/main/java/com/company/banking/
  auth/ | customer/ | account/ | …   # feature packages
  common/ | config/ | security/
src/main/resources/
  application*.yml
  db/migration/                      # Flyway
docker/                              # Compose + Dockerfiles
docs/                                # Product & engineering specs
```

## Contributing / branches

Feature work uses `feat/<name>`, bugfixes `bugfix/<name>`, branched from `main`. Open a PR against `main` after push.

Local-only agent tooling (gitignored, not on remote): `.ai/`, `.cursor/`, `AGENTS.md`, `CLAUDE.md`.
