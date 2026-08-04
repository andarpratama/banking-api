# Database Design Document (DDD)

## Banking API

# 1. Overview

This document defines the logical database design for the Banking API.

# 2. DBMS

-   PostgreSQL 17
-   UTF-8
-   UUID primary keys
-   Flyway migrations

# 3. Entity List

-   users
-   roles
-   user_roles
-   customers
-   accounts
-   transactions
-   refresh_tokens
-   audit_logs
-   notifications

# 4. Entity Details

## users

  Column          Type           Constraints
  --------------- -------------- -----------------
  id              UUID           PK
  email           VARCHAR(255)   UNIQUE NOT NULL
  password_hash   VARCHAR(255)   NOT NULL
  enabled         BOOLEAN        NOT NULL
  created_at      TIMESTAMP      NOT NULL

Indexes: - uq_users_email

## roles

  Column   Type
  -------- -------------
  id       UUID
  name     VARCHAR(50)

Default: - ADMIN - CUSTOMER

## customers

  Column            Type
  ----------------- --------------------
  id                UUID
  customer_number   VARCHAR(20) UNIQUE
  full_name         VARCHAR(150)
  phone             VARCHAR(30)
  address           TEXT
  status            VARCHAR(20)
  user_id           UUID FK users

Relationship: - User 1:1 Customer

## accounts

  Column           Type
  ---------------- --------------------
  id               UUID
  account_number   VARCHAR(20) UNIQUE
  customer_id      UUID FK customers
  account_type     VARCHAR(20)
  currency         CHAR(3)
  balance          DECIMAL(19,2)
  status           VARCHAR(20)
  version          BIGINT

Indexes: - account_number - customer_id

## transactions

  Column             Type
  ------------------ ------------------
  id                 UUID
  account_id         UUID FK accounts
  reference_id       UUID
  transaction_type   VARCHAR(30)
  amount             DECIMAL(19,2)
  balance_after      DECIMAL(19,2)
  description        VARCHAR(255)
  created_at         TIMESTAMP

Rules: - Immutable - Never update/delete

## refresh_tokens

-   id
-   user_id
-   token
-   expires_at
-   revoked

## audit_logs

-   id
-   actor
-   endpoint
-   method
-   action
-   status_code
-   ip_address
-   created_at

## notifications

-   id
-   customer_id
-   title
-   message
-   channel
-   status
-   created_at

# 5. Relationships

Customer (1) ---- (*) Account Account (1) ---- (*) Transaction User (1)
---- (1) Customer User (*) ---- (*) Role

# 6. Constraints

-   Email unique
-   Account number unique
-   Customer number unique
-   Balance \>= 0
-   Currency ISO-4217

# 7. Index Strategy

-   users(email)
-   accounts(account_number)
-   accounts(customer_id)
-   transactions(account_id, created_at)
-   transactions(reference_id)
-   audit_logs(created_at)

# 8. Transaction Strategy

-   @Transactional for deposit, withdraw, transfer
-   Optimistic locking via version
-   Rollback on exception

# 9. Retention

-   Transactions: never deleted
-   Audit logs: retain indefinitely
-   Soft delete for customers

# 10. Future Tables

-   beneficiaries
-   scheduled_payments
-   exchange_rates
-   fraud_cases
