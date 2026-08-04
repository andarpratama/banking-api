# Software Requirements Specification (SRS)

## Banking API

Version: 1.0

# 1. Introduction

## 1.1 Purpose

This SRS defines the complete software requirements for an
enterprise-style Banking API built with Java 21 and Spring Boot.

## 1.2 Scope

The system provides: - Authentication - Customer Management - Account
Management - Deposit - Withdraw - Transfer - Transaction History -
Statements - Dashboard - Audit Logging

# 2. Overall Description

## Product Perspective

RESTful backend exposing JSON APIs.

## User Classes

### Administrator

Manage customers, accounts, dashboard and audit logs.

### Customer

Manage own profile, accounts and transactions.

# 3. System Features

## SF-01 Authentication

### Description

Secure login using JWT.

### Inputs

Email Password

### Outputs

Access Token Refresh Token

### Preconditions

Registered account.

### Postconditions

Authenticated session.

### Validation

-   Email required
-   Password minimum 8 characters

------------------------------------------------------------------------

## SF-02 Customer Management

Operations: - Create - Read - Update - Soft Delete - Search - Pagination

Rules - Email unique - Customer Number generated

------------------------------------------------------------------------

## SF-03 Account Management

Fields

-   Account Number
-   Customer
-   Currency
-   Type
-   Balance
-   Status
-   Version

Status

ACTIVE

FROZEN

CLOSED

Business Rules

-   Cannot delete account.
-   Closed accounts immutable.
-   Multiple accounts allowed.

------------------------------------------------------------------------

## SF-04 Deposit

Preconditions

ACTIVE account

Positive amount

Flow

Validate

Begin transaction

Update balance

Insert transaction

Audit log

Commit

Rollback on failure

------------------------------------------------------------------------

## SF-05 Withdraw

Additional Rules

Cannot exceed balance.

No negative balances.

------------------------------------------------------------------------

## SF-06 Transfer

Requirements

Atomic transaction

Reference ID

Debit source

Credit destination

Ledger entries immutable

Concurrency handled via optimistic locking.

------------------------------------------------------------------------

## SF-07 Transaction History

Supports

Pagination

Sorting

Filtering

Search by reference

------------------------------------------------------------------------

## SF-08 Dashboard

Statistics

Total Customers

Accounts

Deposits

Withdrawals

Transfers

Balances

------------------------------------------------------------------------

## SF-09 Audit Logging

Capture

Actor

IP

Endpoint

HTTP Method

Payload hash

Timestamp

Result

# 4. External Interfaces

REST

JSON

HTTPS

OpenAPI

# 5. Database Requirements

Entities

User

Role

Customer

Account

Transaction

RefreshToken

AuditLog

Notification

Relationships

Customer 1..N Account

Account 1..N Transaction

# 6. Security

JWT

RBAC

BCrypt

Rate Limiting

CORS

Input Validation

Global Exception Handler

# 7. Performance

95% requests \<500ms

Connection Pool

Redis Cache

Indexes

# 8. Reliability

ACID Transactions

Flyway Migration

Health Check

Graceful Shutdown

# 9. Logging

Structured JSON Logging

Correlation ID

Audit Logging

# 10. Error Response

``` json
{
 "timestamp":"",
 "status":400,
 "code":"INVALID_AMOUNT",
 "message":"Amount must be greater than zero"
}
```

# 11. Acceptance Criteria

-   JWT works.
-   RBAC enforced.
-   Transfers atomic.
-   Ledger immutable.
-   Swagger complete.
-   Docker deployment successful.
-   Unit and integration tests passing.
