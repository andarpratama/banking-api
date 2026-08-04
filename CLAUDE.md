# Banking API — Local Rules

> **Agent workflow:** [AGENTS.md](AGENTS.md) · [.ai/prompt.md](.ai/prompt.md) · backlog: [.ai/backlog.md](.ai/backlog.md)  
> **Product / design docs (selektif):** [`docs/`](docs/) — requirements, architecture, database, api, engineering.

## Project overview

Enterprise-grade **Banking REST API** portfolio — Java 21 + Spring Boot, feature-first Clean Architecture, PostgreSQL 17, Redis, JWT auth, audit, Docker.

| Layer | Stack |
|-------|--------|
| Runtime | Java 21, Spring Boot 3.x |
| Build | Maven (`pom.xml`) |
| API | REST JSON, `/api/v1`, OpenAPI / Swagger |
| DB | PostgreSQL 17 + Flyway |
| Cache | Redis |
| Auth | JWT access + refresh rotation, BCrypt |
| Test | JUnit 5, Mockito, AssertJ, Testcontainers |
| Ops | Docker Compose (dev/prod), GitHub Actions (later) |

**Default local port:** API `8080` — base path `/api/v1`. Health: `GET /api/v1/health`.

**Tidak ada di v1 implementasi awal:** frontend React (future consumer), microservices split (modular monolith dulu).

---

## Repository layout (target)

```
.
├── src/main/java/com/company/banking/
│   ├── BankingApplication.java
│   ├── auth/ | customer/ | account/ | transaction/
│   ├── audit/ | dashboard/ | notification/
│   ├── common/ | config/ | security/
│   └── … each feature: presentation / application / domain / infrastructure
├── src/main/resources/
│   ├── application.yml | application-dev.yml | application-prod.yml
│   └── db/migration/          # Flyway
├── src/test/java/…
├── docker/                    # Dockerfile*, compose*, .dockerignore
├── docs/                      # Product & engineering specs
│   ├── requirements/
│   ├── architecture/
│   ├── database/
│   ├── api/
│   ├── engineering/
│   └── assets/
├── .ai/                       # Agent workflow (bukan runtime)
├── pom.xml
├── AGENTS.md
└── CLAUDE.md
```

`.ai/` hanya workflow agent — bukan bagian build/deploy.

---

## Architecture rules (always-on)

1. Controllers → Application only.
2. Domain never depends on Spring / JPA / Redis.
3. Infrastructure implements Domain/Application contracts.
4. No business logic in controllers.
5. Repository interfaces in Domain; implementations in Infrastructure.
6. Cross-feature via Application services or domain events.
7. Immutable ledger/transaction records.
8. Constructor injection only.
9. Package by feature, not by technical layer alone.
10. New features follow the same folder template (`docs/architecture/Project_Structure_Guideline.md`).

Detail layering: [.ai/architecture.md](.ai/architecture.md) + [ADR-0001](docs/architecture/ADR-0001-Architecture-Style.md).

---

## Git identity (commits)

- Author/committer: `andarpratama` `<andar.webdev@gmail.com>`
- Jangan sertakan `Co-authored-by: Cursor` / `cursoragent`
- Satu task selesai = satu commit lokal, **tanpa push** kecuali diminta
- Lihat juga `.cursor/rules/git-commit-identity.mdc`

---

## Conflict resolution

| Topik | Source of truth |
|-------|-----------------|
| Agent workflow / satu task | `AGENTS.md` + `.ai/prompt.md` + task file |
| Struktur package / layer | `docs/architecture/` + `.ai/architecture.md` |
| Kontrak API | `docs/api/Banking_API_OpenAPI_Specification.md` |
| Skema DB | `docs/database/` |
| Testing | `.ai/testing-strategy.md` + `docs/engineering/Banking_API_Testing_Strategy.md` |
