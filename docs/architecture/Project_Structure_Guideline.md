# Project Structure Recommendation

## Banking API (Feature-First Clean Architecture)

## Philosophy

This project adopts a **Feature-First Clean Architecture**. Each
business capability owns its presentation, application, domain, and
infrastructure layers. This keeps modules cohesive, improves
discoverability, and supports future extraction into modular services.

------------------------------------------------------------------------

# Root Structure

``` text
banking-api/
├── .github/
├── docker/
├── docs/
├── scripts/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/company/banking/
│   │   │       ├── auth/
│   │   │       ├── customer/
│   │   │       ├── account/
│   │   │       ├── transaction/
│   │   │       ├── audit/
│   │   │       ├── dashboard/
│   │   │       ├── notification/
│   │   │       ├── common/
│   │   │       ├── config/
│   │   │       ├── security/
│   │   │       └── BankingApplication.java
│   │   └── resources/
│   └── test/
├── pom.xml
└── README.md
```

------------------------------------------------------------------------

# Feature Layout

Every feature follows the same convention.

``` text
account/
├── presentation/
│   ├── controller/
│   ├── request/
│   ├── response/
│   └── swagger/
│
├── application/
│   ├── dto/
│   ├── mapper/
│   ├── service/
│   ├── usecase/
│   └── validator/
│
├── domain/
│   ├── entity/
│   ├── repository/
│   ├── service/
│   ├── valueobject/
│   ├── event/
│   └── exception/
│
└── infrastructure/
    ├── persistence/
    │   ├── entity/
    │   ├── repository/
    │   └── mapper/
    ├── cache/
    ├── client/
    └── configuration/
```

------------------------------------------------------------------------

# Layer Responsibilities

## presentation

-   REST Controllers
-   Request/Response models
-   HTTP validation
-   OpenAPI annotations

Never: - Access database directly - Implement business rules

## application

-   Use Cases
-   Application Services
-   DTO Mapping
-   Transaction orchestration

## domain

-   Entities
-   Value Objects
-   Repository interfaces
-   Business rules
-   Domain services
-   Domain events

No dependency on Spring Boot, JPA, PostgreSQL or Redis.

## infrastructure

-   Spring Data JPA
-   PostgreSQL
-   Redis
-   JWT
-   External APIs
-   Repository implementations

------------------------------------------------------------------------

# Common Module

``` text
common/
├── constants/
├── exception/
├── response/
├── util/
├── validation/
└── mapper/
```

Contains reusable components shared by all features.

------------------------------------------------------------------------

# Security Module

``` text
security/
├── config/
├── filter/
├── jwt/
├── handler/
└── permission/
```

------------------------------------------------------------------------

# Config Module

``` text
config/
├── database/
├── redis/
├── cache/
├── openapi/
├── jackson/
└── async/
```

------------------------------------------------------------------------

# Advantages

-   Feature-first organization
-   High cohesion
-   Low coupling
-   Easy navigation
-   Better scalability
-   Easier testing
-   Supports modular monolith evolution
-   Familiar for developers coming from NestJS while remaining idiomatic
    for modern Spring Boot projects

------------------------------------------------------------------------

# Design Rules

1.  Controllers only call Application layer.
2.  Domain never depends on Spring.
3.  Infrastructure implements Domain/Application contracts.
4.  No business logic inside controllers.
5.  Repositories are interfaces in Domain; implementations live in
    Infrastructure.
6.  Cross-feature communication goes through Application services or
    domain events.
7.  Immutable financial transactions.
8.  Constructor injection only.
9.  Package by feature, not by technical layer.
10. Every new feature follows the same folder template.
