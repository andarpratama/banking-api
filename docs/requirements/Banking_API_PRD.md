# Product Requirements Document (PRD)

# Banking API Portfolio Project

## 1. Product Vision

Build an enterprise-grade Banking REST API that demonstrates modern
backend engineering using Java 21 and Spring Boot. The product simulates
retail banking operations with security, transactional integrity,
auditing, and maintainable architecture.

## 2. Goals

-   Demonstrate production-ready backend architecture.
-   Provide secure banking operations.
-   Showcase testing, documentation, and DevOps practices.
-   Serve as backend for future React frontend.

## 3. Success Metrics

-   100% documented endpoints.

-   =80% service-layer unit test coverage.

-   Integration tests for critical flows.

-   API response \<500ms under normal load.

-   Zero balance inconsistencies during concurrent transfers.

## 4. Target Users

### Customer

Owns one or more accounts and performs banking operations.

### Admin

Maintains customers, accounts, audit logs, and dashboard.

## 5. User Stories

### Authentication

-   As a visitor, I can register.
-   As a user, I can log in.
-   As a user, I can refresh my token.
-   As a user, I can securely log out.

### Customer

-   View/update profile.
-   View owned accounts.
-   View transaction history.
-   Download statement (future).

### Banking

-   Deposit funds.
-   Withdraw funds.
-   Transfer between accounts.
-   View balances.

### Admin

-   Create customers.
-   Freeze/unfreeze accounts.
-   Close accounts.
-   View audit logs.
-   View analytics dashboard.

## 6. Functional Specification

### Module: Authentication

Endpoints: - POST /auth/register - POST /auth/login - POST
/auth/refresh - POST /auth/logout

Validation: - Email unique - Password min 8 chars - BCrypt hashing - JWT
access token - Refresh token rotation

### Module: Customer

Fields: - id - customerNumber - fullName - email - phone - address -
status - createdAt - updatedAt

Capabilities: - CRUD - Soft delete - Search - Pagination - Sorting

### Module: Account

Fields: - accountNumber - type - balance - currency - status - version -
customerId

Rules: - Auto-generated account number - Optimistic locking - One
customer may own many accounts

### Deposit

Flow: 1. Validate account. 2. Validate amount. 3. Begin transaction. 4.
Update balance. 5. Insert immutable transaction. 6. Commit. 7. Audit
log.

### Withdraw

Additional validation: - Balance sufficient. - Account ACTIVE.

### Transfer

Business Flow: 1. Validate source. 2. Validate destination. 3. Prevent
same account. 4. Validate amount. 5. Lock records. 6. Debit source. 7.
Credit destination. 8. Create debit ledger. 9. Create credit ledger. 10.
Shared referenceId. 11. Commit. 12. Audit.

Failure at any step rolls back entire transaction.

### Transactions

Filters: - Date range - Type - Min/max amount - Pagination - Sort

### Dashboard

Widgets: - Total customers - Active accounts - Total balances - Daily
transfers - Daily deposits - Daily withdrawals

## 7. Non-functional Requirements

Security: - JWT - RBAC - BCrypt - Input validation - Global exception
handler

Performance: - Redis cache - Database indexes - Connection pooling

Reliability: - ACID transactions - Flyway migrations - Health endpoint

Observability: - Structured logging - Audit logging - Request
correlation ID

## 8. API Standards

-   JSON
-   REST
-   HTTP status conventions
-   RFC7807-inspired error payload

## 9. Error Codes

-   INVALID_CREDENTIALS
-   CUSTOMER_NOT_FOUND
-   ACCOUNT_NOT_FOUND
-   ACCOUNT_FROZEN
-   ACCOUNT_CLOSED
-   INSUFFICIENT_BALANCE
-   INVALID_AMOUNT
-   DUPLICATE_EMAIL
-   UNAUTHORIZED
-   FORBIDDEN

## 10. UX Notes

Swagger must include: - Request examples - Response examples - Error
examples - Authorization support

## 11. Acceptance Criteria

-   Authentication fully functional.
-   Transfers atomic.
-   Immutable ledger.
-   Audit generated for sensitive operations.
-   Docker Compose runs API, PostgreSQL, Redis.
-   Swagger complete.
-   Tests passing.

## 12. Roadmap

Phase 1: Authentication, Customer, Account.

Phase 2: Deposit, Withdraw, Transfer.

Phase 3: Dashboard, Audit, Notifications.

Phase 4: Kafka, Email, Fraud Detection, Multi-currency.

## 13. Tech Stack

Java 21, Spring Boot 3, Spring Security, Spring Data JPA, PostgreSQL,
Redis, Flyway, MapStruct, Lombok, Docker, OpenAPI, JUnit5, Mockito,
Testcontainers, GitHub Actions.
