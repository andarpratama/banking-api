package com.company.banking.common.presentation;

/**
 * Realistic Swagger / OpenAPI example payloads shared by presentation annotations.
 * Values are documentation-only (not used at runtime).
 */
public final class OpenApiExamples {

    public static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";
    public static final String CUSTOMER_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
    public static final String ACCOUNT_ID = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
    public static final String DESTINATION_ACCOUNT_ID = "6b8e2c1a-4d5f-4a90-b3c1-9e7d6f5a4b3c";
    public static final String DEPOSIT_TX_ID = "1a2b3c4d-5e6f-4789-a012-3456789abcde";
    public static final String WITHDRAW_TX_ID = "2b3c4d5e-6f70-489a-b123-456789abcdef";
    public static final String DEBIT_TX_ID = "3c4d5e6f-7081-49ab-c234-56789abcdef0";
    public static final String CREDIT_TX_ID = "4d5e6f70-8192-4abc-d345-6789abcdef01";
    public static final String TRANSFER_REF_ID = "9c8b7a6d-5e4f-4a3b-2c1d-0e9f8a7b6c5d";
    public static final String AUDIT_ID = "0e1f2a3b-4c5d-4678-9abc-def012345678";

    public static final String ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjdXN0b21lckBleGFtcGxlLmNvbSIsInJvbGVzIjpbIkNVU1RPTUVSIl19.example";
    public static final String REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjdXN0b21lckBleGFtcGxlLmNvbSIsInR5cCI6InJlZnJlc2gifQ.example";

    public static final String HEALTH_UP = """
            {
              "status": "UP"
            }
            """;

    public static final String READINESS_UP = """
            {
              "status": "UP",
              "database": "UP",
              "cache": "UP"
            }
            """;

    public static final String READINESS_DOWN = """
            {
              "status": "DOWN",
              "database": "DOWN",
              "cache": "UP"
            }
            """;

    public static final String REGISTER_REQUEST = """
            {
              "email": "customer@example.com",
              "password": "SecurePass123!",
              "fullName": "John Doe",
              "phone": "+1-555-0123",
              "address": "123 Main St, City, State 12345"
            }
            """;

    public static final String REGISTER_RESPONSE = """
            {
              "id": "550e8400-e29b-41d4-a716-446655440000",
              "email": "customer@example.com",
              "fullName": "John Doe",
              "customerId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
              "createdAt": "2026-08-04T12:00:00Z"
            }
            """;

    public static final String LOGIN_REQUEST = """
            {
              "email": "customer@example.com",
              "password": "SecurePass123!"
            }
            """;

    public static final String LOGIN_RESPONSE = """
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
            """;

    public static final String REFRESH_REQUEST = """
            {
              "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjdXN0b21lckBleGFtcGxlLmNvbSIsInR5cCI6InJlZnJlc2gifQ.example"
            }
            """;

    public static final String TOKEN_RESPONSE = """
            {
              "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjdXN0b21lckBleGFtcGxlLmNvbSIsInJvbGVzIjpbIkNVU1RPTUVSIl19.example",
              "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjdXN0b21lckBleGFtcGxlLmNvbSIsInR5cCI6InJlZnJlc2gifQ.example",
              "tokenType": "Bearer",
              "expiresIn": 3600
            }
            """;

    public static final String CUSTOMER_RESPONSE = """
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
            """;

    public static final String CUSTOMER_UPDATED_RESPONSE = """
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
            """;

    public static final String CUSTOMER_LIST_RESPONSE = """
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
            """;

    public static final String UPDATE_CUSTOMER_REQUEST = """
            {
              "fullName": "John Doe Updated",
              "phone": "+1-555-0456",
              "address": "456 Oak Ave"
            }
            """;

    public static final String CREATE_ACCOUNT_REQUEST = """
            {
              "customerId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
              "accountType": "SAVINGS",
              "currency": "USD",
              "initialBalance": 1000.00
            }
            """;

    public static final String ACCOUNT_CREATED_RESPONSE = """
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
            """;

    public static final String ACCOUNT_RESPONSE = """
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
            """;

    public static final String ACCOUNT_LIST_RESPONSE = """
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
            """;

    public static final String ACCOUNT_FROZEN_RESPONSE = """
            {
              "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "accountNumber": "ACC-0000001",
              "status": "FROZEN",
              "updatedAt": "2026-08-04T13:00:00Z"
            }
            """;

    public static final String ACCOUNT_UNFROZEN_RESPONSE = """
            {
              "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "accountNumber": "ACC-0000001",
              "status": "ACTIVE",
              "updatedAt": "2026-08-04T13:05:00Z"
            }
            """;

    public static final String ACCOUNT_CLOSED_RESPONSE = """
            {
              "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "accountNumber": "ACC-0000001",
              "status": "CLOSED",
              "updatedAt": "2026-08-04T13:10:00Z"
            }
            """;

    public static final String DEPOSIT_REQUEST = """
            {
              "accountId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "amount": 500.00,
              "description": "Cash deposit at ATM"
            }
            """;

    public static final String DEPOSIT_RESPONSE = """
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
            """;

    public static final String WITHDRAW_REQUEST = """
            {
              "accountId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "amount": 300.00,
              "description": "ATM withdrawal"
            }
            """;

    public static final String WITHDRAW_RESPONSE = """
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
            """;

    public static final String TRANSFER_REQUEST = """
            {
              "sourceAccountId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "destinationAccountId": "6b8e2c1a-4d5f-4a90-b3c1-9e7d6f5a4b3c",
              "amount": 250.00,
              "description": "Transfer to friend"
            }
            """;

    public static final String TRANSFER_RESPONSE = """
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
            """;

    public static final String TRANSACTION_HISTORY_RESPONSE = """
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
            """;

    public static final String STATEMENT_RESPONSE = """
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
            """;

    public static final String DASHBOARD_METRICS_RESPONSE = """
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
            """;

    public static final String AUDIT_LOG_LIST_RESPONSE = """
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
            """;

    public static final String ERROR_DUPLICATE_EMAIL = """
            {
              "timestamp": "2026-08-04T12:00:00Z",
              "status": 400,
              "code": "DUPLICATE_EMAIL",
              "message": "Email already registered",
              "path": "/api/v1/auth/register"
            }
            """;

    public static final String ERROR_INVALID_CREDENTIALS = """
            {
              "timestamp": "2026-08-04T12:00:00Z",
              "status": 401,
              "code": "INVALID_CREDENTIALS",
              "message": "Invalid email or password",
              "path": "/api/v1/auth/login"
            }
            """;

    public static final String ERROR_INVALID_TOKEN = """
            {
              "timestamp": "2026-08-04T12:00:00Z",
              "status": 401,
              "code": "INVALID_TOKEN",
              "message": "Refresh token expired or invalid",
              "path": "/api/v1/auth/refresh"
            }
            """;

    public static final String ERROR_UNAUTHORIZED = """
            {
              "timestamp": "2026-08-04T12:00:00Z",
              "status": 401,
              "code": "UNAUTHORIZED",
              "message": "Authentication required",
              "path": "/api/v1/customers"
            }
            """;

    public static final String ERROR_FORBIDDEN = """
            {
              "timestamp": "2026-08-04T12:00:00Z",
              "status": 403,
              "code": "FORBIDDEN",
              "message": "Insufficient permissions",
              "path": "/api/v1/customers"
            }
            """;

    public static final String ERROR_CUSTOMER_NOT_FOUND = """
            {
              "timestamp": "2026-08-04T12:00:00Z",
              "status": 404,
              "code": "CUSTOMER_NOT_FOUND",
              "message": "Customer not found",
              "path": "/api/v1/customers/7c9e6679-7425-40de-944b-e07fc1f90ae7"
            }
            """;

    public static final String ERROR_ACCOUNT_NOT_FOUND = """
            {
              "timestamp": "2026-08-04T12:00:00Z",
              "status": 404,
              "code": "ACCOUNT_NOT_FOUND",
              "message": "Account not found",
              "path": "/api/v1/accounts/3fa85f64-5717-4562-b3fc-2c963f66afa6"
            }
            """;

    public static final String ERROR_INVALID_AMOUNT = """
            {
              "timestamp": "2026-08-04T12:00:00Z",
              "status": 400,
              "code": "INVALID_AMOUNT",
              "message": "Amount must be greater than zero",
              "path": "/api/v1/transactions/deposit"
            }
            """;

    public static final String ERROR_ACCOUNT_FROZEN = """
            {
              "timestamp": "2026-08-04T12:00:00Z",
              "status": 409,
              "code": "ACCOUNT_FROZEN",
              "message": "Cannot transact on frozen account",
              "path": "/api/v1/transactions/deposit"
            }
            """;

    public static final String ERROR_INSUFFICIENT_BALANCE = """
            {
              "timestamp": "2026-08-04T12:00:00Z",
              "status": 409,
              "code": "INSUFFICIENT_BALANCE",
              "message": "Insufficient balance. Available: 100.00, Requested: 300.00",
              "path": "/api/v1/transactions/withdraw"
            }
            """;

    public static final String ERROR_SAME_ACCOUNT_TRANSFER = """
            {
              "timestamp": "2026-08-04T12:00:00Z",
              "status": 409,
              "code": "SAME_ACCOUNT_TRANSFER",
              "message": "Cannot transfer to same account",
              "path": "/api/v1/transactions/transfer"
            }
            """;

    public static final String ERROR_OPTIMISTIC_LOCK = """
            {
              "timestamp": "2026-08-04T12:00:00Z",
              "status": 409,
              "code": "OPTIMISTIC_LOCK_EXCEPTION",
              "message": "Account was modified by another transaction",
              "path": "/api/v1/transactions/transfer"
            }
            """;

    private OpenApiExamples() {
    }
}
