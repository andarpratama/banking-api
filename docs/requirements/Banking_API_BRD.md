# Business Requirements Document (BRD)

# Banking API System

## 1. Purpose

Build a RESTful Banking API that simulates core retail banking
operations using Java 21 and Spring Boot. The system demonstrates
enterprise-grade architecture, security, transactional consistency,
testing, and maintainability.

## 2. Objectives

-   Secure authentication and authorization.
-   Customer and account management.
-   Deposit, withdrawal, transfer.
-   Immutable transaction history.
-   Audit trail.
-   REST API with OpenAPI.
-   High code quality and automated tests.

## 3. Stakeholders

  Role                 Responsibility
  -------------------- -----------------------------------------------
  Admin                Manage customers/accounts, monitor system
  Customer             Own accounts and perform banking transactions
  Developer            Build and maintain APIs
  Recruiter/Reviewer   Evaluate architecture and implementation

## 4. Scope

### In Scope

-   Authentication (JWT + Refresh Token)
-   RBAC
-   Customer CRUD
-   Account CRUD
-   Deposit
-   Withdraw
-   Transfer
-   Transaction History
-   Account Statement
-   Dashboard
-   Audit Log
-   Notifications (mock)
-   Swagger
-   Docker
-   Testing

### Out of Scope

-   Real payment gateway
-   Inter-bank transfer
-   KYC verification
-   Mobile application

## 5. Functional Requirements

### FR-01 Authentication

-   Register customer
-   Login
-   Refresh token
-   Logout
-   Password hashing (BCrypt)

### FR-02 Customer Management

Fields: - Customer ID - Full Name - Email - Phone - Address - Status -
Created At

Rules: - Email unique - Soft delete only

### FR-03 Account Management

Fields: - Account Number - Account Type (Savings, Checking) - Balance -
Currency - Status - Version (optimistic locking)

Rules: - One customer may own multiple accounts. - Account number
generated automatically. - Initial balance \>= 0.

### FR-04 Deposit

Rules: - Amount \> 0 - Creates immutable transaction record - Updates
balance atomically

### FR-05 Withdraw

Rules: - Amount \> 0 - Insufficient balance rejected - Account must be
ACTIVE

### FR-06 Transfer

Rules: - Source != destination - Same currency - Atomic transaction -
Debit and credit must both succeed or rollback - Store shared reference
ID

### FR-07 Transaction History

Filters: - Date range - Type - Amount Pagination and sorting required.

### FR-08 Statement

Return: - Opening balance - Transactions - Closing balance

### FR-09 Dashboard

Metrics: - Total customers - Active accounts - Daily transfer volume -
Total deposits - Total withdrawals

### FR-10 Audit Log

Capture: - User - Endpoint - Action - Timestamp - Result

## 6. Business Rules

1.  Balance may never become negative.
2.  Transactions are immutable.
3.  Every balance change generates one transaction.
4.  Transfer creates two ledger entries (debit/credit).
5.  Closed accounts cannot transact.
6.  Frozen accounts allow viewing only.
7.  Admin cannot transfer customer funds.

## 7. Non-Functional Requirements

-   Java 21
-   Spring Boot 3
-   PostgreSQL
-   Redis
-   JWT
-   Flyway
-   Docker
-   Response \<500ms (normal load)
-   80%+ unit test coverage target
-   OWASP security practices

## 8. Roles & Permissions

### ADMIN

-   Manage customers
-   Freeze/unfreeze account
-   Close account
-   View dashboard
-   View audit logs

### CUSTOMER

-   View profile
-   Deposit
-   Withdraw
-   Transfer
-   View statements

## 9. Main Entities

-   User
-   Role
-   Customer
-   Account
-   Transaction
-   RefreshToken
-   AuditLog
-   Notification

Relationships: - Customer 1..\* Account - Account 1..\* Transaction

## 10. API Modules

-   /auth
-   /customers
-   /accounts
-   /transactions
-   /transfers
-   /dashboard
-   /audit

## 11. Error Handling

Standard JSON:

``` json
{
  "timestamp":"",
  "status":400,
  "code":"INSUFFICIENT_BALANCE",
  "message":"Balance is insufficient"
}
```

## 12. Acceptance Criteria

-   Secure JWT authentication
-   Atomic transfers
-   Complete Swagger documentation
-   Docker Compose starts full stack
-   Flyway migrations succeed
-   Unit and integration tests pass
-   README explains setup and architecture

## 13. Future Enhancements

-   Kafka event publishing
-   Email notifications
-   Multi-currency
-   Scheduled payments
-   Fraud detection
-   Virtual accounts
