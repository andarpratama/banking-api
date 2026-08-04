# Banking API — Agent Instructions

Proyek: enterprise Banking REST API (portfolio) — Java 21 + Spring Boot, Clean Architecture feature-first, PostgreSQL, Redis.

## Workflow (wajib)

1. Baca `.ai/backlog.md` — section **Current task** saja.
2. Baca task file yang di-link (contoh: `.ai/tasks/01-maven-spring-scaffold.md`) — **utuh**.
3. Ikuti **DoD applies** dan **Architecture sections** di task file — jangan baca `definition-of-done.md` / `architecture.md` utuh.
4. Kerjakan **satu task** saja. Stop setelah selesai + update backlog + commit lokal.

## Prinsip (ringkas)

- Feature-first Clean Architecture: `src/main/java/com/company/banking/<feature>/{presentation,application,domain,infrastructure}`.
- Domain **tidak** bergantung pada Spring / JPA / Redis.
- Controllers hanya memanggil Application layer — no business logic di controller.
- Constructor injection only.
- Immutable financial transactions; optimistic locking pada balance.
- Spec produk & API: `docs/` (requirements, architecture, api, …) — baca **selektif** per References di task file.

## Jangan load berlebihan

- ❌ `@.ai/` (seluruh folder)
- ❌ `architecture.md` utuh (kecuali task cross-cutting)
- ❌ `definition-of-done.md` utuh (pakai **DoD applies** di task file)
- ❌ Semua file di `docs/` sekaligus (pakai References di task file)
- ✅ Task file + section DoD/architecture yang listed

Detail lengkap: [.ai/prompt.md](.ai/prompt.md)
