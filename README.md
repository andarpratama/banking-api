# banking-api

Enterprise Banking REST API (portfolio) — Java 21, Spring Boot, feature-first Clean Architecture, PostgreSQL, Redis.

## Docs

See [docs/README.md](docs/README.md) for BRD/PRD/SRS, ADR, OpenAPI, testing, security, and deployment guides (organized under `docs/` subfolders).

Docker files live under [`docker/`](docker/).

## Agent entrypoints

| File | Purpose |
|------|---------|
| [AGENTS.md](AGENTS.md) | Short agent entry instructions |
| [CLAUDE.md](CLAUDE.md) | Always-on technical rules |

Local AI task workflow lives in **`.ai/`** (gitignored). Keep that folder on your machine; it is not pushed to the remote.
