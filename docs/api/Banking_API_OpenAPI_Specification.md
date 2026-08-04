# Banking API - OpenAPI Specification

**Version:** 1.0.0  
**Date:** 2026-08-04  
**Base URL:** `http://localhost:8080/api/v1`

---

## 0. System Endpoints

### 0.1 Health Check

```
GET /health
```

Public — no authentication required (must stay on the security whitelist when JWT is enabled).

Response 200 OK:
```json
{
  "status": "UP"
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
  "id": "uuid",
  "email": "customer@example.com",
  "fullName": "John Doe",
  "customerId": "uuid",
  "createdAt": "2026-08-04T12:00:00Z"
}

Response 400 Bad Request:
{
  "timestamp": "2026-08-04T12:00:00Z",
  "status": 400,
  "code": "DUPLICATE_EMAIL",
  "message": "Email already registered"
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
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "uuid",
    "email": "customer@example.com",
    "roles": ["CUSTOMER"]
  }
}

Response 401 Unauthorized:
{
  "timestamp": "2026-08-04T12:00:00Z",
  "status": 401,
  "code": "INVALID_CREDENTIALS",
  "message": "Invalid email or password"
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
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}

Response 200 OK:
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}

Response 401 Unauthorized:
{
  "timestamp": "2026-08-04T12:00:00Z",
  "status": 401,
  "code": "INVALID_TOKEN",
  "message": "Refresh token expired or invalid"
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
      "id": "uuid",
      "customerNumber": "CUST-000001",
      "fullName": "John Doe",
      "email": "john@example.com",
      "phone": "+1-555-0123",
      "address": "123 Main St",
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
  "id": "uuid",
  "customerNumber": "CUST-000001",
  "fullName": "John Doe",
  "email": "john@example.com",
  "phone": "+1-555-0123",
  "address": "123 Main St",
  "status": "ACTIVE",
  "createdAt": "2026-08-04T12:00:00Z",
  "updatedAt": "2026-08-04T12:00:00Z"
}

Response 404 Not Found:
{
  "timestamp": "2026-08-04T12:00:00Z",
  "status": 404,
  "code": "CUSTOMER_NOT_FOUND",
  "message": "Customer not found"
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
  "id": "uuid",
  "customerNumber": "CUST-000001",
  "fullName": "John Doe Updated",
  "email": "john@example.com",
  "phone": "+1-555-0456",
  "address": "456 Oak Ave",
  "status": "ACTIVE",
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
  "customerId": "uuid",
  "accountType": "SAVINGS",
  "currency": "USD",
  "initialBalance": 1000.00
}

Response 201 Created:
{
  "id": "uuid",
  "accountNumber": "ACC-0000001",
  "customerId": "uuid",
  "accountType": "SAVINGS",
  "currency": "USD",
  "balance": 1000.00,
  "status": "ACTIVE",
  "version": 1,
  "createdAt": "2026-08-04T12:00:00Z"
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
      "id": "uuid",
      "accountNumber": "ACC-0000001",
      "customerId": "uuid",
      "accountType": "SAVINGS",
      "currency": "USD",
      "balance": 5000.50,
      "status": "ACTIVE",
      "version": 10,
      "createdAt": "2026-08-04T12:00:00Z"
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
  "id": "uuid",
  "accountNumber": "ACC-0000001",
  "customerId": "uuid",
  "accountType": "SAVINGS",
  "currency": "USD",
  "balance": 5000.50,
  "status": "ACTIVE",
  "version": 10,
  "createdAt": "2026-08-04T12:00:00Z"
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
  "id": "uuid",
  "accountNumber": "ACC-0000001",
  "status": "FROZEN",
  "updatedAt": "2026-08-04T13:00:00Z"
}
```

---

### 3.5 Close Account (Admin)

```
PATCH /accounts/{accountId}/close
Authorization: Bearer {accessToken}
Roles: ADMIN

Response 200 OK:
{
  "id": "uuid",
  "accountNumber": "ACC-0000001",
  "status": "CLOSED",
  "updatedAt": "2026-08-04T13:00:00Z"
}
```

---

## 4. Transaction Endpoints

### 4.1 Deposit

```
POST /transactions/deposit
Content-Type: application/json
Authorization: Bearer {accessToken}

Request Body:
{
  "accountId": "uuid",
  "amount": 500.00,
  "description": "Cash deposit at ATM"
}

Response 200 OK:
{
  "id": "uuid",
  "accountId": "uuid",
  "referenceId": null,
  "transactionType": "DEPOSIT",
  "amount": 500.00,
  "balanceAfter": 5500.50,
  "description": "Cash deposit at ATM",
  "createdAt": "2026-08-04T12:00:00Z"
}

Response 400 Bad Request:
{
  "status": 400,
  "code": "INVALID_AMOUNT",
  "message": "Amount must be greater than zero"
}

Response 409 Conflict:
{
  "status": 409,
  "code": "ACCOUNT_FROZEN",
  "message": "Cannot transact on frozen account"
}
```

---

### 4.2 Withdraw

```
POST /transactions/withdraw
Content-Type: application/json
Authorization: Bearer {accessToken}

Request Body:
{
  "accountId": "uuid",
  "amount": 300.00,
  "description": "ATM withdrawal"
}

Response 200 OK:
{
  "id": "uuid",
  "accountId": "uuid",
  "transactionType": "WITHDRAW",
  "amount": 300.00,
  "balanceAfter": 5200.50,
  "description": "ATM withdrawal",
  "createdAt": "2026-08-04T12:05:00Z"
}

Response 409 Conflict:
{
  "status": 409,
  "code": "INSUFFICIENT_BALANCE",
  "message": "Insufficient balance. Available: 100.00, Requested: 300.00"
}
```

---

### 4.3 Transfer

```
POST /transactions/transfer
Content-Type: application/json
Authorization: Bearer {accessToken}

Request Body:
{
  "sourceAccountId": "uuid-source",
  "destinationAccountId": "uuid-dest",
  "amount": 250.00,
  "description": "Transfer to friend"
}

Response 200 OK:
{
  "referenceId": "REF-abc123def456",
  "sourceTransaction": {
    "id": "uuid",
    "accountId": "uuid-source",
    "transactionType": "DEBIT",
    "amount": 250.00,
    "balanceAfter": 4950.50,
    "createdAt": "2026-08-04T12:10:00Z"
  },
  "destinationTransaction": {
    "id": "uuid",
    "accountId": "uuid-dest",
    "transactionType": "CREDIT",
    "amount": 250.00,
    "balanceAfter": 5500.50,
    "createdAt": "2026-08-04T12:10:00Z"
  }
}

Response 409 Conflict:
{
  "status": 409,
  "code": "SAME_ACCOUNT_TRANSFER",
  "message": "Cannot transfer to same account"
}

Response 409 Conflict:
{
  "status": 409,
  "code": "OPTIMISTIC_LOCK_EXCEPTION",
  "message": "Account was modified by another transaction"
}
```

---

## 5. Transaction History Endpoints

### 5.1 Get Transactions for Account

```
GET /accounts/{accountId}/transactions?page=0&size=20&sort=createdAt,desc&type=DEPOSIT&fromDate=2026-08-01&toDate=2026-08-04&minAmount=0&maxAmount=10000
Authorization: Bearer {accessToken}

Query Parameters:
- page (default: 0): Page number
- size (default: 20): Page size
- sort (default: createdAt,desc): Sort field and direction
- type: DEPOSIT, WITHDRAW, DEBIT, CREDIT
- fromDate: ISO 8601 format
- toDate: ISO 8601 format
- minAmount: Minimum amount filter
- maxAmount: Maximum amount filter

Response 200 OK:
{
  "content": [
    {
      "id": "uuid",
      "accountId": "uuid",
      "referenceId": "REF-abc123def456",
      "transactionType": "DEPOSIT",
      "amount": 500.00,
      "balanceAfter": 5500.50,
      "description": "Cash deposit",
      "createdAt": "2026-08-04T12:00:00Z"
    },
    {
      "id": "uuid",
      "accountId": "uuid",
      "referenceId": "REF-xyz789",
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
```

---

### 5.2 Get Statement for Account

```
GET /accounts/{accountId}/statement?fromDate=2026-08-01&toDate=2026-08-04
Authorization: Bearer {accessToken}

Response 200 OK:
{
  "accountId": "uuid",
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
      "id": "uuid",
      "date": "2026-08-01T10:00:00Z",
      "type": "DEPOSIT",
      "amount": 500.00,
      "balance": 5000.50,
      "description": "Cash deposit"
    }
  ]
}
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

---

## 7. Audit Log Endpoints

### 7.1 Get Audit Logs (Admin)

```
GET /audit-logs?page=0&size=20&sort=createdAt,desc&actor=john@example.com&endpoint=/transactions/transfer&status=SUCCESS
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
      "id": "uuid",
      "actor": "john@example.com",
      "endpoint": "/transactions/transfer",
      "method": "POST",
      "action": "TRANSFER_MONEY",
      "statusCode": 200,
      "status": "SUCCESS",
      "ipAddress": "192.168.1.100",
      "payloadHash": "sha256hash...",
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

---

## 9. Security

### Authentication
- JWT Bearer token in `Authorization` header
- Access token expiration: 1 hour
- Refresh token expiration: 7 days
- Public (no auth): `GET /health`

### Authorization
- RBAC with ADMIN and CUSTOMER roles
- Customer can only access own resources
- Admin can access all resources

### Headers
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 10. Rate Limiting

- 100 requests per minute per user
- Rate limit headers in response:
  - `X-RateLimit-Limit: 100`
  - `X-RateLimit-Remaining: 95`
  - `X-RateLimit-Reset: 1691145660`

---

## 11. Versioning

API version: v1

Future versions maintained as `/api/v2`, etc.

---
