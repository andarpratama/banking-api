# ADR-0001: Architecture Style

-   **Status:** Accepted
-   **Date:** 2026-08-03
-   **Decision Makers:** Project Owner
-   **Related:** BRD, PRD, SRS, DDD

------------------------------------------------------------------------

# 1. Context

The Banking API is intended as an enterprise-grade portfolio project
demonstrating production-ready backend engineering with Java 21 and
Spring Boot.

The system must satisfy:

-   High maintainability
-   Strong separation of concerns
-   Testability
-   Transactional consistency
-   Scalability
-   Clear domain boundaries
-   Technology independence
-   Future migration to microservices without major rewrites

The application is expected to grow from a few endpoints to many
business capabilities including authentication, customer management,
accounts, transfers, notifications, audit logging, reporting, fraud
detection, and scheduled jobs.

------------------------------------------------------------------------

# 2. Problem Statement

Choosing the wrong architecture early will lead to:

-   Business logic leaking into controllers
-   Tight coupling to Spring Framework
-   Difficult testing
-   Large God Services
-   Vendor lock-in
-   Low maintainability
-   Difficult migration to distributed systems

Therefore an architecture must be selected before implementation.

------------------------------------------------------------------------

# 3. Decision

The project will adopt **Clean Architecture** inspired by Robert C.
Martin with practical adaptations for Spring Boot.

The implementation will use four logical layers:

``` text
Presentation
    │
Application
    │
Domain
    │
Infrastructure
```

Dependency rule:

``` text
Presentation
      ↓
Application
      ↓
Domain

Infrastructure → implements interfaces defined by Application/Domain
```

The Domain layer must not depend on Spring Boot, JPA, Hibernate,
PostgreSQL, Redis, or any external framework.

------------------------------------------------------------------------

# 4. Architecture Goals

1.  Independent of frameworks.
2.  Independent of database technology.
3.  Independent of delivery mechanism (REST today, GraphQL/gRPC
    tomorrow).
4.  Highly testable.
5.  Explicit business rules.
6.  Replaceable infrastructure.

------------------------------------------------------------------------

# 5. Layer Responsibilities

## Presentation

Responsibilities

-   REST Controllers
-   DTO validation
-   Authentication entry points
-   HTTP mapping
-   Exception translation

Must NOT

-   contain business rules
-   access repositories directly

------------------------------------------------------------------------

## Application

Responsibilities

-   Use Cases
-   Transaction boundaries
-   Authorization orchestration
-   Business workflows
-   Domain event publication

Typical classes

-   TransferMoneyUseCase
-   DepositMoneyUseCase
-   LoginUseCase

------------------------------------------------------------------------

## Domain

Contains:

-   Entities
-   Value Objects
-   Domain Services
-   Repository interfaces
-   Domain Events
-   Business Rules

No Spring annotations should exist here except where unavoidable by
language constraints.

------------------------------------------------------------------------

## Infrastructure

Responsibilities

-   Spring Data JPA
-   PostgreSQL
-   Redis
-   JWT
-   Email
-   Logging
-   External APIs

Infrastructure implements interfaces owned by inner layers.

------------------------------------------------------------------------

# 6. Package Structure

``` text
com.example.banking

common/
config/

auth/

customer/

account/

transaction/

audit/

notification/
```

Within each feature:

``` text
account

application
domain
infrastructure
presentation
```

Feature-first packaging is preferred over layer-first packaging.

------------------------------------------------------------------------

# 7. Why Clean Architecture?

Benefits:

-   Excellent unit testing
-   Lower coupling
-   Better readability
-   Easier onboarding
-   Clear ownership of business rules
-   Long-term maintainability

------------------------------------------------------------------------

# 8. Alternatives Considered

## Layered Architecture

Pros

-   Easy
-   Familiar

Cons

-   Often creates God Service classes
-   Weak domain isolation

Decision: Rejected.

## Hexagonal Architecture

Pros

-   Strong dependency inversion
-   Excellent ports/adapters

Cons

-   Slightly higher complexity for portfolio.

Decision: Concepts adopted where useful.

## Microservices

Pros

-   Independent deployment

Cons

-   Operational complexity
-   Premature optimization

Decision: Rejected for initial release.

------------------------------------------------------------------------

# 9. Architectural Principles

-   SOLID
-   DRY
-   KISS
-   Dependency Inversion
-   Fail Fast
-   Explicit Transactions
-   Immutable Ledger
-   Domain-driven naming

------------------------------------------------------------------------

# 10. Risks

  Risk                    Mitigation
  ----------------------- ---------------------------------------
  Too many abstractions   Keep interfaces only where beneficial
  Learning curve          Consistent package templates
  Feature growth          Modular feature packaging

------------------------------------------------------------------------

# 11. Consequences

Positive

-   Easier testing
-   Better code organization
-   Easier refactoring
-   Technology replacement possible

Negative

-   More files
-   More initial design effort

------------------------------------------------------------------------

# 12. Future Evolution

The architecture supports future migration to:

-   Event-driven architecture
-   Kafka
-   CQRS
-   Read models
-   Microservices
-   Distributed tracing

without major changes to the Domain layer.

------------------------------------------------------------------------

# 13. Decision Summary

The Banking API shall adopt a Clean Architecture with feature-based
modular packaging. Business rules remain framework-independent while
Spring Boot, JPA, Redis, PostgreSQL, and external integrations are
isolated in Infrastructure. This maximizes maintainability, testability,
and extensibility while remaining practical for a production-style
portfolio.
