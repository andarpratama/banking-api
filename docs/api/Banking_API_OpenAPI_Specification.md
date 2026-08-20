# Banking API - OpenAPI Specification

**Version:** 1.0.0  
**Date:** 2026-08-04  
**Base URL:** `http://localhost:8080/api/v1`

Sample identifiers used throughout request/response examples (documentation only):

| Resource | Example value |
|----------|----------------|
| User id | `550e8400-e29b-41d4-a716-446655440000` |
| Customer id | `7c9e6679-7425-40de-944b-e07fc1f90ae7` |
| Savings account | `3fa85f64-5717-4562-b3fc-2c963f66afa6` (`ACC-0000001`) |
| Destination account | `6b8e2c1a-4d5f-4a90-b3c1-9e7d6f5a4b3c` |
| Transfer reference | `9c8b7a6d-5e4f-4a3b-2c1d-0e9f8a7b6c5d` |

---

## 0. System Endpoints

### 0.1 Health Check (Legacy)

```
GET /health
```

Public — no authentication required (must stay on the security whitelist when JWT is enabled).

**Deprecated:** Use `/health/live` or `/health/ready` for Kubernetes probes.

Response 200 OK:
```json
{
  "status": "UP"
}
```

---

### 0.2 Liveness Probe (Kubernetes)

```
GET /health/live
```

Public — Kubernetes liveness probe. Fast check (< 10ms) that the application process is running.  
Does NOT validate dependencies. Used by K8s to detect dead pods and restart them.

Response 200 OK:
```json
{
  "status": "UP"
}
```

---

### 0.3 Readiness Probe (Kubernetes)

```
GET /health/ready
```

Public — Kubernetes readiness probe. Validates critical dependencies (PostgreSQL, Redis).  
Used by K8s to route traffic only to ready pods.

Response 200 OK (ready to serve traffic):
```json
{
  "status": "UP",
  "database": "UP",
  "cache": "UP"
}
```

Response 503 Service Unavailable (not ready):
```json
{
  "status": "DOWN",
  "database": "DOWN",
  "cache": "UP"
}
```

---

## 1. Authentication Endpoints

### 1.1 Register

```
POST /auth/register
Content-Type: application/json

Request Body:
{
  "email": "customer@example.com",
  "password": "SecurePass123!",
  "fullName": "John Doe",
  "phone": "+1-555-0123",
  "address": "123 Main St, City, State 12345"
}

Response 201 Created:
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "customer@example.com",
  "fullName": "John Doe",
  "customerId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "createdAt": "2026-08-04T12:00:00Z"
}

Response 400 Bad Request:
{
  "timestamp": "2026-08-04T12:00:00Z",
  "status": 400,
  "code": "DUPLICATE_EMAIL",
  "message": "Email already registered",
  "path": "/api/v1/auth/register"
}
```

---

### 1.2 Login

```
POST /auth/login
Content-Type: application/json

Request Body:
{
  "email": "customer@example.com",
  "password": "SecurePass123!"
}

Response 200 OK:
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjdXN0b21lckBleGFtcGxlLmNvbSIsInJvbGVzIjpbIkNVU1RPTUVSIl19.example",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjdXN0b21lckBleGFtcGxlLmNvbSIsInR5cCI6InJlZnJlc2gifQ.example",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "customer@example.com",
    "roles": ["CUSTOMER"]
  }
}

Response 401 Unauthorized:
{
  "timestamp": "2026-08-04T12:00:00Z",
  "status": 401,
  "code": "INVALID_CREDENTIALS",
  "message": "Invalid email or password",
  "path": "/api/v1/auth/login"
}
```

---

### 1.3 Refresh Token

```
POST /auth/refresh
Content-Type: application/json
Authorization: Bearer {refreshToken}

Request Body:
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjdXN0b21lckBleGFtcGxlLmNvbSIsInR5cCI6InJlZnJlc2gifQ.example"
}

Response 200 OK:
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjdXN0b21lckBleGFtcGxlLmNvbSIsInJvbGVzIjpbIkNVU1RPTUVSIl19.example",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjdXN0b21lckBleGFtcGxlLmNvbSIsInR5cCI6InJlZnJlc2gifQ.example",
  "tokenType": "Bearer",
  "expiresIn": 3600
}

Response 401 Unauthorized:
{
  "timestamp": "2026-08-04T12:00:00Z",
  "status": 401,
  "code": "INVALID_TOKEN",
  "message": "Refresh token expired or invalid",
  "path": "/api/v1/auth/refresh"
}
```

---

### 1.4 Logout

```
POST /auth/logout
Authorization: Bearer {accessToken}

Response 204 No Content
```

---

## 2. Customer Endpoints

### 2.1 Get All Customers (Admin)

```
GET /customers?page=0&size=20&sort=createdAt,desc
Authorization: Bearer {accessToken}
Roles: ADMIN

Response 200 OK:
{
  "content": [
    {
      "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "customerNumber": "CUST-000001",
      "fullName": "John Doe",
      "email": "customer@example.com",
      "phone": "+1-555-0123",
      "address": "123 Main St, City, State 12345",
      "status": "ACTIVE",
      "createdAt": "2026-08-04T12:00:00Z",
      "updatedAt": "2026-08-04T12:00:00Z"
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "currentPage": 0,
  "pageSize": 20
}
```

---

### 2.2 Get Customer by ID

```
GET /customers/{customerId}
Authorization: Bearer {accessToken}
Roles: ADMIN, CUSTOMER (own profile only)

Response 200 OK:
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "customerNumber": "CUST-000001",
  "fullName": "John Doe",
  "email": "customer@example.com",
  "phone": "+1-555-0123",
  "address": "123 Main St, City, State 12345",
  "status": "ACTIVE",
  "createdAt": "2026-08-04T12:00:00Z",
  "updatedAt": "2026-08-04T12:00:00Z"
}

Response 404 Not Found:
{
  "timestamp": "2026-08-04T12:00:00Z",
  "status": 404,
  "code": "CUSTOMER_NOT_FOUND",
  "message": "Customer not found",
  "path": "/api/v1/customers/7c9e6679-7425-40de-944b-e07fc1f90ae7"
}
```

---

### 2.3 Update Customer

```
PUT /customers/{customerId}
Content-Type: application/json
Authorization: Bearer {accessToken}
Roles: ADMIN, CUSTOMER (own profile only)

Request Body:
{
  "fullName": "John Doe Updated",
  "phone": "+1-555-0456",
  "address": "456 Oak Ave"
}

Response 200 OK:
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "customerNumber": "CUST-000001",
  "fullName": "John Doe Updated",
  "email": "customer@example.com",
  "phone": "+1-555-0456",
  "address": "456 Oak Ave",
  "status": "ACTIVE",
  "createdAt": "2026-08-04T12:00:00Z",
  "updatedAt": "2026-08-04T13:00:00Z"
}
```

---

### 2.4 Soft Delete Customer

```
DELETE /customers/{customerId}
Authorization: Bearer {accessToken}
Roles: ADMIN

Response 204 No Content
```

---

## 3. Account Endpoints

### 3.1 Create Account

```
POST /accounts
Content-Type: application/json
Authorization: Bearer {accessToken}

Request Body:
{
  "customerId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "accountType": "SAVINGS",
  "currency": "USD",
  "initialBalance": 1000.00
}

Response 201 Created:
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "accountNumber": "ACC-0000001",
  "customerId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "accountType": "SAVINGS",
  "currency": "USD",
  "balance": 1000.00,
  "status": "ACTIVE",
  "version": 0,
  "createdAt": "2026-08-04T12:00:00Z",
  "updatedAt": "2026-08-04T12:00:00Z"
}
```

---

### 3.2 Get All Accounts for Customer

```
GET /customers/{customerId}/accounts
Authorization: Bearer {accessToken}
Roles: ADMIN, CUSTOMER (own accounts only)

Response 200 OK:
{
  "content": [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "accountNumber": "ACC-0000001",
      "customerId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "accountType": "SAVINGS",
      "currency": "USD",
      "balance": 5000.50,
      "status": "ACTIVE",
      "version": 10,
      "createdAt": "2026-08-04T12:00:00Z",
      "updatedAt": "2026-08-04T12:10:00Z"
    }
  ],
  "totalElements": 3
}
```

---

### 3.3 Get Account by ID

```
GET /accounts/{accountId}
Authorization: Bearer {accessToken}

Response 200 OK:
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "accountNumber": "ACC-0000001",
  "customerId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "accountType": "SAVINGS",
  "currency": "USD",
  "balance": 5000.50,
  "status": "ACTIVE",
  "version": 10,
  "createdAt": "2026-08-04T12:00:00Z",
  "updatedAt": "2026-08-04T12:10:00Z"
}
```

---

### 3.4 Freeze Account (Admin)

```
PATCH /accounts/{accountId}/freeze
Authorization: Bearer {accessToken}
Roles: ADMIN

Response 200 OK:
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "accountNumber": "ACC-0000001",
  "status": "FROZEN",
  "updatedAt": "2026-08-04T13:00:00Z"
}
```

---

### 3.5 Unfreeze Account (Admin)

```
PATCH /accounts/{accountId}/unfreeze
Authorization: Bearer {accessToken}
Roles: ADMIN

Transitions FROZEN → ACTIVE only. Rejects ACTIVE (400 VALIDATION_ERROR) and CLOSED (409 ACCOUNT_CLOSED).

Response 200 OK:
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "accountNumber": "ACC-0000001",
  "status": "ACTIVE",
  "updatedAt": "2026-08-04T13:05:00Z"
}
```

---

### 3.6 Close Account (Admin)

```
PATCH /accounts/{accountId}/close
Authorization: Bearer {accessToken}
Roles: ADMIN

Response 200 OK:
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "accountNumber": "ACC-0000001",
  "status": "CLOSED",
  "updatedAt": "2026-08-04T13:10:00Z"
}
```

---

## 4. Transaction Endpoints

### 4.1 Deposit

```
POST /transactions/deposit
Content-Type: application/json
Authorization: Bearer {accessToken}

Roles: ADMIN, CUSTOMER (own account only)

Request Body:
{
  "accountId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "amount": 500.00,
  "description": "Cash deposit at ATM"
}

Response 200 OK:
{
  "id": "1a2b3c4d-5e6f-4789-a012-3456789abcde",
  "accountId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "referenceId": null,
  "transactionType": "DEPOSIT",
  "amount": 500.00,
  "balanceAfter": 5500.50,
  "description": "Cash deposit at ATM",
  "createdAt": "2026-08-04T12:00:00Z"
}

Response 400 Bad Request:
{
  "timestamp": "2026-08-04T12:00:00Z",
  "status": 400,
  "code": "INVALID_AMOUNT",
  "message": "Amount must be greater than zero",
  "path": "/api/v1/transactions/deposit"
}

Response 409 Conflict:
{
  "timestamp": "2026-08-04T12:00:00Z",
  "status": 409,
  "code": "ACCOUNT_FROZEN",
  "message": "Cannot transact on frozen account",
  "path": "/api/v1/transactions/deposit"
}

Response 409 Conflict:
{
  "timestamp": "2026-08-04T12:00:00Z",
  "status": 409,
  "code": "ACCOUNT_CLOSED",
  "message": "Cannot transact on closed account",
  "path": "/api/v1/transactions/deposit"
}
```

---

### 4.2 Withdraw

```
POST /transactions/withdraw
Content-Type: application/json
Authorization: Bearer {accessToken}

Roles: ADMIN, CUSTOMER (own account only)

Request Body:
{
  "accountId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "amount": 300.00,
  "description": "ATM withdrawal"
}

Response 200 OK:
{
  "id": "2b3c4d5e-6f70-489a-b123-456789abcdef",
  "accountId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "referenceId": null,
  "transactionType": "WITHDRAW",
  "amount": 300.00,
  "balanceAfter": 5200.50,
  "description": "ATM withdrawal",
  "createdAt": "2026-08-04T12:05:00Z"
}

Response 400 Bad Request:
{
  "timestamp": "2026-08-04T12:00:00Z",
  "status": 400,
  "code": "INVALID_AMOUNT",
  "message": "Amount must be greater than zero",
  "path": "/api/v1/transactions/withdraw"
}

Response 409 Conflict:
{
  "timestamp": "2026-08-04T12:00:00Z",
  "status": 409,
  "code": "INSUFFICIENT_BALANCE",
  "message": "Insufficient balance. Available: 100.00, Requested: 300.00",
  "path": "/api/v1/transactions/withdraw"
}

Response 409 Conflict:
{
  "timestamp": "2026-08-04T12:00:00Z",
  "status": 409,
  "code": "ACCOUNT_FROZEN",
  "message": "Cannot transact on frozen account",
  "path": "/api/v1/transactions/withdraw"
}

Response 409 Conflict:
{
  "timestamp": "2026-08-04T12:00:00Z",
  "status": 409,
  "code": "ACCOUNT_CLOSED",
  "message": "Cannot transact on closed account",
  "path": "/api/v1/transactions/withdraw"
}
```

---

### 4.3 Transfer

```
POST /transactions/transfer
Content-Type: application/json
Authorization: Bearer {accessToken}

Roles: ADMIN, CUSTOMER (own source account only)

Request Body:
{
  "sourceAccountId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "destinationAccountId": "6b8e2c1a-4d5f-4a90-b3c1-9e7d6f5a4b3c",
  "amount": 250.00,
  "description": "Transfer to friend"
}

Response 200 OK:
{
  "referenceId": "9c8b7a6d-5e4f-4a3b-2c1d-0e9f8a7b6c5d",
  "sourceTransaction": {
    "id": "3c4d5e6f-7081-49ab-c234-56789abcdef0",
    "accountId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "referenceId": "9c8b7a6d-5e4f-4a3b-2c1d-0e9f8a7b6c5d",
    "transactionType": "DEBIT",
    "amount": 250.00,
    "balanceAfter": 4950.50,
    "description": "Transfer to friend",
    "createdAt": "2026-08-04T12:10:00Z"
  },
  "destinationTransaction": {
    "id": "4d5e6f70-8192-4abc-d345-6789abcdef01",
    "accountId": "6b8e2c1a-4d5f-4a90-b3c1-9e7d6f5a4b3c",
    "referenceId": "9c8b7a6d-5e4f-4a3b-2c1d-0e9f8a7b6c5d",
    "transactionType": "CREDIT",
    "amount": 250.00,
    "balanceAfter": 5500.50,
    "description": "Transfer to friend",
    "createdAt": "2026-08-04T12:10:00Z"
  }
}

Response 409 Conflict:
{
  "timestamp": "2026-08-04T12:00:00Z",
  "status": 409,
  "code": "SAME_ACCOUNT_TRANSFER",
  "message": "Cannot transfer to same account",
  "path": "/api/v1/transactions/transfer"
}

Response 409 Conflict:
{
  "timestamp": "2026-08-04T12:00:00Z",
  "status": 409,
  "code": "OPTIMISTIC_LOCK_EXCEPTION",
  "message": "Account was modified by another transaction",
  "path": "/api/v1/transactions/transfer"
}
```

Notes:
- Account balances use JPA `@Version` optimistic locking. On conflict the API returns
  `OPTIMISTIC_LOCK_EXCEPTION` (409). The server does **not** auto-retry; clients should
  re-read balances and resubmit if appropriate.
- Dual ledger: one `DEBIT` (source) and one `CREDIT` (destination) sharing the same
  `referenceId` (UUID). Entire transfer is a single DB transaction (atomic rollback).

---

## 5. Transaction History Endpoints

### 5.1 Get Transactions for Account

```
GET /accounts/{accountId}/transactions?page=0&size=20&sort=createdAt,desc&type=DEPOSIT&fromDate=2026-08-01&toDate=2026-08-04&minAmount=0&maxAmount=10000
Authorization: Bearer {accessToken}
Roles: ADMIN or account owner (CUSTOMER)

Query Parameters:
- page (default: 0): Page number
- size (default: 20): Page size (max 100)
- sort (default: createdAt,desc): Sort field and direction — allowed fields: createdAt, amount, transactionType
- type: DEPOSIT, WITHDRAW, DEBIT, CREDIT (optional)
- fromDate: ISO 8601 date (yyyy-MM-dd), inclusive start of day UTC (optional)
- toDate: ISO 8601 date (yyyy-MM-dd), inclusive end of day UTC (optional)
- minAmount: Minimum amount filter (optional)
- maxAmount: Maximum amount filter (optional)

Response 200 OK:
{
  "content": [
    {
      "id": "1a2b3c4d-5e6f-4789-a012-3456789abcde",
      "accountId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "referenceId": null,
      "transactionType": "DEPOSIT",
      "amount": 500.00,
      "balanceAfter": 5500.50,
      "description": "Cash deposit at ATM",
      "createdAt": "2026-08-04T12:00:00Z"
    },
    {
      "id": "3c4d5e6f-7081-49ab-c234-56789abcdef0",
      "accountId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "referenceId": "9c8b7a6d-5e4f-4a3b-2c1d-0e9f8a7b6c5d",
      "transactionType": "DEBIT",
      "amount": 250.00,
      "balanceAfter": 5250.50,
      "description": "Transfer to friend",
      "createdAt": "2026-08-04T12:10:00Z"
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "currentPage": 0,
  "pageSize": 20
}

Errors: 400 validation, 401 unauthorized, 403 not owner, 404 account not found
```

---

### 5.2 Get Statement for Account

```
GET /accounts/{accountId}/statement?fromDate=2026-08-01&toDate=2026-08-04
Authorization: Bearer {accessToken}
Roles: ADMIN or account owner (CUSTOMER)

Query Parameters:
- fromDate: ISO 8601 date (yyyy-MM-dd), inclusive start of day UTC (required)
- toDate: ISO 8601 date (yyyy-MM-dd), inclusive end of day UTC (required)

Notes:
- openingBalance is the balanceAfter of the latest ledger row strictly before fromDate (0.00 if none)
- closingBalance is the balanceAfter of the last ledger row in the period (openingBalance if none)
- transactions are ordered oldest-first by createdAt
- totalDeposits / totalWithdrawals / totalDebits / totalCredits sum amounts by type in the period

Response 200 OK:
{
  "accountId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "accountNumber": "ACC-0000001",
  "statementPeriod": {
    "from": "2026-08-01T00:00:00Z",
    "to": "2026-08-04T23:59:59Z"
  },
  "openingBalance": 4500.50,
  "closingBalance": 5700.50,
  "totalDeposits": 1500.00,
  "totalWithdrawals": 300.00,
  "totalDebits": 250.00,
  "totalCredits": 250.00,
  "transactions": [
    {
      "id": "1a2b3c4d-5e6f-4789-a012-3456789abcde",
      "date": "2026-08-01T10:00:00Z",
      "type": "DEPOSIT",
      "amount": 500.00,
      "balance": 5000.50,
      "description": "Cash deposit at ATM"
    }
  ]
}

Errors: 400 validation (missing dates or fromDate after toDate), 401 unauthorized, 403 not owner, 404 account not found
```

---

## 6. Dashboard Endpoints

### 6.1 Get Dashboard Metrics (Admin)

```
GET /dashboard/metrics
Authorization: Bearer {accessToken}
Roles: ADMIN

Response 200 OK:
{
  "totalCustomers": 1250,
  "activeCustomers": 980,
  "totalAccounts": 2150,
  "activeAccounts": 1890,
  "totalBalance": 5234567.89,
  "daily": {
    "deposits": {
      "count": 456,
      "amount": 125000.00
    },
    "withdrawals": {
      "count": 234,
      "amount": 87500.00
    },
    "transfers": {
      "count": 345,
      "amount": 234567.89
    }
  },
  "weekly": {
    "deposits": {
      "count": 3200,
      "amount": 890000.00
    },
    "withdrawals": {
      "count": 1600,
      "amount": 620000.00
    },
    "transfers": {
      "count": 2400,
      "amount": 1650000.00
    }
  }
}
```

Notes (v1):
- `activeCustomers`: customers with `is_deleted = false`.
- `activeAccounts`: accounts with status `ACTIVE`.
- `totalBalance`: `SUM(accounts.balance)`.
- Volume windows use UTC calendar days: **daily** = from start of today UTC; **weekly** = from start of (today − 6 days) UTC through now.
- `deposits` / `withdrawals` map to ledger types `DEPOSIT` / `WITHDRAW`.
- `transfers` count/amount use ledger type `DEBIT` only (paired `CREDIT` rows are not double-counted).

---

## 7. Audit Log Endpoints

### 7.1 Get Audit Logs (Admin)

```
GET /audit-logs?page=0&size=20&sort=createdAt,desc&actor=customer@example.com&endpoint=/transactions/transfer&status=SUCCESS
Authorization: Bearer {accessToken}
Roles: ADMIN

Query Parameters:
- page, size, sort: Pagination
- actor: User email filter
- endpoint: API endpoint filter
- status: SUCCESS, FAILURE
- fromDate, toDate: Date range filter

Response 200 OK:
{
  "content": [
    {
      "id": "0e1f2a3b-4c5d-4678-9abc-def012345678",
      "actor": "customer@example.com",
      "endpoint": "/transactions/transfer",
      "method": "POST",
      "action": "TRANSFER_MONEY",
      "statusCode": 200,
      "status": "SUCCESS",
      "ipAddress": "192.168.1.100",
      "payloadHash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      "createdAt": "2026-08-04T12:10:00Z"
    }
  ],
  "totalElements": 5000,
  "totalPages": 250,
  "currentPage": 0,
  "pageSize": 20
}
```

---

## 8. Error Response Format

### Standard Error Response

```json
{
  "timestamp": "2026-08-04T12:00:00Z",
  "status": 400,
  "code": "ERROR_CODE",
  "message": "Human-readable error message",
  "path": "/api/v1/endpoint"
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| INVALID_CREDENTIALS | 401 | Email or password incorrect |
| INVALID_TOKEN | 401 | Access token invalid or expired |
| UNAUTHORIZED | 401 | Missing or invalid authorization |
| FORBIDDEN | 403 | Insufficient permissions |
| CUSTOMER_NOT_FOUND | 404 | Customer does not exist |
| ACCOUNT_NOT_FOUND | 404 | Account does not exist |
| ACCOUNT_FROZEN | 409 | Account is frozen |
| ACCOUNT_CLOSED | 409 | Account is closed |
| INSUFFICIENT_BALANCE | 409 | Not enough funds |
| INVALID_AMOUNT | 400 | Amount is zero or negative |
| DUPLICATE_EMAIL | 400 | Email already registered |
| SAME_ACCOUNT_TRANSFER | 409 | Source and destination are same |
| OPTIMISTIC_LOCK_EXCEPTION | 409 | Concurrent modification detected |
| RATE_LIMIT_EXCEEDED | 429 | Too many requests in the current window |

---

## 9. Security

### Authentication
- JWT Bearer token in `Authorization` header
- Access token expiration: 1 hour
- Refresh token expiration: 7 days
- Public (no auth required at filter): `GET /health`, `GET /health/live`, `GET /health/ready`, `POST /auth/**`
- `POST /auth/logout` still requires a valid Bearer access token (validated in the auth service; token is blacklisted on success)
- Refresh rotation: previous refresh token hash is revoked when a new pair is issued

### Authorization
- RBAC with ADMIN and CUSTOMER roles
- Customer can only access own resources
- Admin can access all resources

### Headers
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjdXN0b21lckBleGFtcGxlLmNvbSIsInJvbGVzIjpbIkNVU1RPTUVSIl19.example
```

Response security headers (v1):
- `Strict-Transport-Security: max-age=31536000 ; includeSubDomains` (effective on HTTPS)
- `Content-Security-Policy: default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'; form-action 'self'`
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Referrer-Policy: no-referrer`
- `Permissions-Policy: camera=(), microphone=(), geolocation=()`

---

## 10. Rate Limiting

- Global: **100 requests per minute** per authenticated user (fallback: client IP)
- Auth endpoints (`/api/v1/auth/**`): **20 requests per minute** per client IP (stricter anti-abuse)
- Excluded: `GET /api/v1/health`, `GET /api/v1/health/live`, `GET /api/v1/health/ready`, Swagger UI / OpenAPI docs
- Backend: Redis (default, multi-instance) or in-memory (`app.rate-limit.backend=memory`)
- On exceed: HTTP **429** with code `RATE_LIMIT_EXCEEDED`
- Rate limit headers in response:
  - `X-RateLimit-Limit: 100`
  - `X-RateLimit-Remaining: 95`
  - `X-RateLimit-Reset: 1691145660`
  - `Retry-After: <seconds>` (when limited)

---

## 11. Versioning

API version: v1

Future versions maintained as `/api/v2`, etc.

---

## 12. Schema Validation (CI)

Runtime JSON is checked against the springdoc document `GET /v3/api-docs` in `OpenApiSchemaValidationTest` (T-091). That test also asserts every v1 path in this markdown file is present in the generated spec.

Swagger UI `@ExampleObject` payloads for all v1 endpoints match the samples in this file (shared identifiers in the header table). Product contract source of truth remains this file; springdoc annotations must stay aligned when endpoints change.

Coverage is asserted by `OpenApiExamplesTest` (JSON example constants parse; every mapped controller method has a 2xx `@ExampleObject` unless the status is 204).

---
