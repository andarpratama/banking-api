# Enhanced Database Design Document (DDD)

**Version:** 2.0  
**Date:** 2026-08-04  
**Status:** Comprehensive

---

## 1. Overview & Database Standards

### 1.1 DBMS Selection

- **Primary:** PostgreSQL 17
- **Encoding:** UTF-8
- **Primary Key Strategy:** UUIDv7 (time-sortable)
- **Timezone:** UTC
- **Extensions:** pg_stat_statements, uuid-ossp

### 1.2 Design Principles

1. **ACID Compliance** - All transactions respect atomicity, consistency, isolation, durability
2. **Immutable Ledger** - Transactions never updated or deleted
3. **Optimistic Locking** - Prevent concurrent modification via version fields
4. **Normalization** - 3NF minimum, denormalization only for performance
5. **Constraints & Validation** - Database enforces business rules
6. **Audit Trail** - All changes logged

### 1.3 Naming Conventions

| Object | Convention | Example |
|--------|-----------|---------|
| Table | snake_case, singular | `user`, `customer`, `account` |
| Column | snake_case, descriptive | `user_id`, `created_at`, `account_number` |
| Primary Key | id | `id UUID PRIMARY KEY` |
| Foreign Key | `{table}_id` | `user_id`, `customer_id` |
| Index | `idx_{table}_{columns}` | `idx_users_email`, `idx_accounts_customer_id` |
| Constraint | `chk_{table}_{name}` | `chk_accounts_positive_balance` |
| Unique Constraint | `uq_{table}_{columns}` | `uq_users_email`, `uq_customers_number` |

---

## 2. Conceptual ERD (Entity Relationship Diagram)

```
┌─────────────────────────────────────────────────────────────┐
│                        USER DOMAIN                          │
├─────────────────────────────────────────────────────────────┤
│  users (1) ──── (*) user_roles ──── (1) roles              │
│   ├─ id                                                      │
│   ├─ email (UNIQUE)                                         │
│   ├─ password_hash                                          │
│   └─ enabled                                                 │
└─────────────────────────────────────────────────────────────┘
          │
          │ (1:1)
          ↓
┌─────────────────────────────────────────────────────────────┐
│                     CUSTOMER DOMAIN                         │
├─────────────────────────────────────────────────────────────┤
│  customers (1) ──── (*) accounts ──── (*) transactions      │
│   ├─ id                                                      │
│   ├─ customer_number (UNIQUE)                               │
│   ├─ full_name                                              │
│   ├─ phone                                                   │
│   ├─ address                                                 │
│   ├─ status (ACTIVE|SOFT_DELETED)                           │
│   └─ is_deleted (soft delete)                               │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                     ACCOUNT DOMAIN                          │
├─────────────────────────────────────────────────────────────┤
│  accounts                                                    │
│   ├─ id (UUID)                                              │
│   ├─ account_number (UNIQUE)                                │
│   ├─ customer_id (FK)                                       │
│   ├─ account_type (SAVINGS|CHECKING)                        │
│   ├─ currency (USD|EUR|GBP)                                 │
│   ├─ balance (DECIMAL 19,2)                                 │
│   ├─ status (ACTIVE|FROZEN|CLOSED)                          │
│   ├─ version (BIGINT - optimistic locking)                  │
│   └─ created_at, updated_at                                 │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                   TRANSACTION DOMAIN                        │
├─────────────────────────────────────────────────────────────┤
│  transactions (immutable ledger)                             │
│   ├─ id (UUID)                                              │
│   ├─ account_id (FK)                                        │
│   ├─ reference_id (for transfers)                           │
│   ├─ transaction_type (DEPOSIT|WITHDRAW|DEBIT|CREDIT)       │
│   ├─ amount (DECIMAL 19,2)                                  │
│   ├─ balance_after (snapshot)                               │
│   ├─ description                                             │
│   └─ created_at (immutable)                                  │
│   *** NEVER UPDATE OR DELETE ***                            │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    SUPPORT TABLES                           │
├─────────────────────────────────────────────────────────────┤
│  refresh_tokens     - Token rotation & revocation           │
│  audit_logs        - All sensitive operations               │
│  notifications     - User notifications                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Core Entities - Detailed Specifications

### 3.1 Users Table

**Purpose:** Authentication and authorization base entity

**Schema:**
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
```

**Columns Detail:**

| Column | Type | Constraints | Notes |
|--------|------|-----------|-------|
| id | UUID | PRIMARY KEY | Unique identifier, UUIDv7 |
| email | VARCHAR(255) | NOT NULL, UNIQUE | Login credential, normalized (lowercase) |
| password_hash | VARCHAR(255) | NOT NULL | BCrypt hash (never plain text) |
| enabled | BOOLEAN | NOT NULL, DEFAULT true | Soft disable without deletion |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Immutable creation time |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Last modification time |

**Business Rules:**
- Email must be unique and valid (regex validated in app)
- Password always stored as BCrypt hash (cost >= 10)
- Cannot delete user (soft delete only via customer status)
- Email indexed for fast login lookups

**Sample Queries:**
```sql
-- Find user by email (login)
SELECT * FROM users WHERE email = 'user@example.com';

-- Find active users created last 30 days
SELECT * FROM users WHERE created_at >= NOW() - INTERVAL '30 days' AND enabled = true;
```

---

### 3.2 Roles Table

**Purpose:** Role-based access control (RBAC)

**Schema:**
```sql
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO roles (name, description) VALUES
    ('ADMIN', 'Administrator with full system access'),
    ('CUSTOMER', 'Regular customer with limited access');
```

**Predefined Roles:**
1. **ADMIN** - Full system access (customer mgmt, freeze accounts, view analytics)
2. **CUSTOMER** - Limited access (own accounts, transactions only)

---

### 3.3 User-Roles Junction Table

**Purpose:** Map users to roles (many-to-many)

**Schema:**
```sql
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);
```

**Business Rules:**
- One user can have multiple roles
- Roles are managed by admin only
- Changing roles effective immediately

---

### 3.4 Customers Table

**Purpose:** Customer profile and lifecycle management

**Schema:**
```sql
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    customer_number VARCHAR(20) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(30),
    address TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customers_customer_number ON customers(customer_number);
CREATE INDEX idx_customers_user_id ON customers(user_id);
CREATE INDEX idx_customers_status ON customers(status);
```

**Columns Detail:**

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | Customer unique identifier |
| user_id | UUID | 1:1 relationship with users |
| customer_number | VARCHAR(20) | Auto-generated unique number (CUST-000001) |
| full_name | VARCHAR(150) | Customer's full name |
| phone | VARCHAR(30) | Contact number |
| address | TEXT | Mailing address |
| status | VARCHAR(20) | ACTIVE, SUSPENDED, CLOSED |
| is_deleted | BOOLEAN | Soft delete flag (never hard delete) |
| created_at | TIMESTAMP | Record creation time |
| updated_at | TIMESTAMP | Last modification time |

**Business Rules:**
- One user = One customer (1:1)
- Customer number auto-generated (sequence: CUST-000001, CUST-000002, etc.)
- Soft delete only (set is_deleted=true)
- Cannot delete if has active accounts
- Email sourced from linked user record

**Lifecycle:**
1. Created when user registers
2. Can be updated by customer or admin
3. Can be suspended (soft-deleted) but records retained forever
4. Status tracked for compliance

**Sample Queries:**
```sql
-- Get active customers
SELECT * FROM customers WHERE status = 'ACTIVE' AND is_deleted = false;

-- Get customers by status
SELECT COUNT(*) as total FROM customers WHERE status = 'ACTIVE';

-- Find customer by number
SELECT * FROM customers WHERE customer_number = 'CUST-000001';
```

---

### 3.5 Accounts Table

**Purpose:** Customer bank accounts with balance tracking

**Schema:**
```sql
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    account_type VARCHAR(20) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    balance DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_accounts_account_number ON accounts(account_number);
CREATE INDEX idx_accounts_customer_id ON accounts(customer_id);
CREATE INDEX idx_accounts_status ON accounts(status);

-- Constraints
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_positive_balance 
    CHECK (balance >= 0);
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_valid_currency 
    CHECK (currency ~ '^[A-Z]{3}$');
```

**Columns Detail:**

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | Account unique identifier |
| customer_id | UUID | Parent customer (FK) |
| account_number | VARCHAR(20) | Unique account number (ACC-0000001) |
| account_type | VARCHAR(20) | SAVINGS \| CHECKING |
| currency | CHAR(3) | ISO 4217 code (USD, EUR, GBP) |
| balance | DECIMAL(19,2) | Current balance (always >= 0) |
| status | VARCHAR(20) | ACTIVE \| FROZEN \| CLOSED |
| version | BIGINT | Optimistic locking version |
| created_at | TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | Last update time |

**Business Rules:**
- One customer can have multiple accounts
- Account number auto-generated and immutable
- Balance never negative (constraint enforced)
- Status transitions: ACTIVE → FROZEN ↔ ACTIVE or ACTIVE → CLOSED
- Cannot delete account (soft delete only via status)
- Optimistic locking: version incremented on each update
- Cannot transact on FROZEN or CLOSED accounts

**Status Lifecycle:**

```
    ACTIVE (default)
    /      \
   /        \ (freeze)
  ↓          ↓
ACTIVE      FROZEN
   \        /
    \ (unfreeze)
     ↓
   CLOSED (final, irreversible)
```

**Optimistic Locking Strategy:**
```sql
-- Update balance with version check (ConcurrentModificationException if fails)
UPDATE accounts 
SET balance = balance + 100.00, version = version + 1
WHERE id = 'account-uuid' AND version = 5;
-- If version doesn't match, UPDATE returns 0 rows
```

**Sample Queries:**
```sql
-- Get all accounts for customer
SELECT * FROM accounts WHERE customer_id = 'customer-uuid' AND status = 'ACTIVE';

-- Get account with optimistic lock check
SELECT * FROM accounts WHERE id = 'acc-uuid' FOR UPDATE;

-- Count active accounts
SELECT COUNT(*) FROM accounts WHERE status = 'ACTIVE';

-- Total balance across all accounts
SELECT SUM(balance) FROM accounts WHERE status = 'ACTIVE';
```

---

## 4. Transaction & Audit Entities

### 4.1 Transactions Table (Immutable Ledger)

**Purpose:** Immutable record of all financial operations

**Schema:**
```sql
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    reference_id UUID,
    transaction_type VARCHAR(30) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    balance_after DECIMAL(19,2) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_reference_id ON transactions(reference_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_account_date ON transactions(account_id, created_at DESC);
```

**Columns Detail:**

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | Transaction unique identifier |
| account_id | UUID | Parent account (FK) |
| reference_id | UUID | For transfers: shared ID between debit/credit |
| transaction_type | VARCHAR(30) | DEPOSIT \| WITHDRAW \| DEBIT \| CREDIT |
| amount | DECIMAL(19,2) | Transaction amount (always positive) |
| balance_after | DECIMAL(19,2) | Account balance snapshot after transaction |
| description | VARCHAR(255) | User-provided description |
| created_at | TIMESTAMP | Immutable creation timestamp |

**Transaction Types:**
1. **DEPOSIT** - Money added to account
2. **WITHDRAW** - Money removed from account
3. **DEBIT** - Money out (transfer source)
4. **CREDIT** - Money in (transfer destination)

**Business Rules:**
- ✅ IMMUTABLE: Never update or delete
- Reference_id connects transfer debit/credit pairs
- Amount always positive (sign in transaction_type)
- Balance_after is a snapshot (for statement reconstruction)
- All amounts logged in original currency
- Created_at represents exact time transaction committed

**Immutability Enforcement:**
```sql
-- Prevent any updates
REVOKE UPDATE ON transactions FROM banking_role;

-- Prevent any deletes (except admins for corrections)
REVOKE DELETE ON transactions FROM banking_role;

-- Audit trail via triggers (if needed)
CREATE TRIGGER audit_transaction_delete
BEFORE DELETE ON transactions
FOR EACH ROW
EXECUTE FUNCTION audit_transaction_change();
```

**Transfer Flow Example:**
```
Transfer: $100 from Account A to Account B

Transaction 1 (DEBIT):
  id: txn-001
  reference_id: transfer-123
  account_id: account-a
  transaction_type: DEBIT
  amount: 100.00
  balance_after: 400.00
  
Transaction 2 (CREDIT):
  id: txn-002
  reference_id: transfer-123  (← same reference)
  account_id: account-b
  transaction_type: CREDIT
  amount: 100.00
  balance_after: 900.00
  
Query: Find related transactions
SELECT * FROM transactions WHERE reference_id = 'transfer-123';
```

**Sample Queries:**
```sql
-- Get all transactions for account (paginated)
SELECT * FROM transactions 
WHERE account_id = 'acc-uuid'
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;

-- Get transactions in date range
SELECT * FROM transactions
WHERE account_id = 'acc-uuid'
  AND created_at BETWEEN '2026-08-01' AND '2026-08-31'
ORDER BY created_at DESC;

-- Get all transfers (debit + credit pairs)
SELECT * FROM transactions
WHERE reference_id = 'transfer-uuid'
ORDER BY transaction_type;

-- Calculate daily deposits
SELECT DATE(created_at) as date, SUM(amount) as total_deposits
FROM transactions
WHERE transaction_type = 'DEPOSIT'
GROUP BY DATE(created_at)
ORDER BY date DESC;

-- Account statement reconstruction
SELECT 
    created_at,
    transaction_type,
    amount,
    balance_after,
    description
FROM transactions
WHERE account_id = 'acc-uuid'
ORDER BY created_at;
```

---

### 4.2 Audit Logs Table

**Purpose:** Security and compliance logging of all sensitive operations

**Schema:**
```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor VARCHAR(255) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL,
    action VARCHAR(100) NOT NULL,
    status_code INTEGER NOT NULL,
    ip_address VARCHAR(45),
    payload_hash VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_actor ON audit_logs(actor);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_endpoint ON audit_logs(endpoint);
```

**Columns Detail:**

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | Log entry identifier |
| actor | VARCHAR(255) | User email or system identifier |
| endpoint | VARCHAR(255) | API endpoint called |
| method | VARCHAR(10) | HTTP method (GET, POST, PUT, DELETE) |
| action | VARCHAR(100) | Business action (TRANSFER_MONEY, FREEZE_ACCOUNT) |
| status_code | INTEGER | HTTP response code |
| ip_address | VARCHAR(45) | Client IP (IPv4 or IPv6) |
| payload_hash | VARCHAR(255) | SHA-256 hash of request body (for tampering detection) |
| created_at | TIMESTAMP | When action occurred |

**Audited Actions (Examples):**
- LOGIN, LOGOUT
- CREATE_ACCOUNT, FREEZE_ACCOUNT, CLOSE_ACCOUNT
- DEPOSIT, WITHDRAW, TRANSFER
- UPDATE_CUSTOMER
- VIEW_DASHBOARD, VIEW_AUDIT_LOGS

**Retention Policy:**
- Retain indefinitely (7+ years for banking compliance)
- Never delete (only admins can, via governance process)
- Archive to cold storage after 1 year if needed

**Sample Queries:**
```sql
-- Recent login failures
SELECT * FROM audit_logs
WHERE action = 'LOGIN' AND status_code = 401
ORDER BY created_at DESC
LIMIT 10;

-- All actions by user in last 24 hours
SELECT * FROM audit_logs
WHERE actor = 'user@example.com'
  AND created_at >= NOW() - INTERVAL '1 day'
ORDER BY created_at DESC;

-- Detect suspicious activity (multiple failed logins)
SELECT actor, COUNT(*) as failed_attempts
FROM audit_logs
WHERE action = 'LOGIN' AND status_code = 401
  AND created_at >= NOW() - INTERVAL '1 hour'
GROUP BY actor
HAVING COUNT(*) > 5;

-- Compliance report: all transfers in month
SELECT * FROM audit_logs
WHERE action = 'TRANSFER'
  AND created_at >= '2026-08-01'
  AND created_at < '2026-09-01'
ORDER BY created_at;
```

---

### 4.3 Refresh Tokens Table

**Purpose:** JWT refresh token management and rotation

**Schema:**
```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
```

**Business Rules:**
- One active refresh token per user (old revoked on new generation)
- Expires after 7 days
- Can be manually revoked (logout)
- Never displayed in logs
- Token itself stored as hash (not plain)

**Rotation Flow:**
```
1. User logs in → access token (1h) + refresh token (7d)
2. Access token expires → use refresh token to get new access token
3. New refresh token issued → old one revoked
4. User logs out → refresh token revoked
```

---

### 4.4 Notifications Table

**Purpose:** User notification system

**Schema:**
```sql
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    channel VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP
);

CREATE INDEX idx_notifications_customer_id ON notifications(customer_id);
CREATE INDEX idx_notifications_status ON notifications(status);
```

**Notification Types:**
- TRANSFER_COMPLETED
- LARGE_WITHDRAWAL
- LOW_BALANCE_ALERT
- ACCOUNT_FROZEN
- PASSWORD_CHANGED

---

## 5. Index Strategy for Performance

### 5.1 Critical Indexes

| Table | Columns | Purpose | Usage |
|-------|---------|---------|-------|
| users | email | Login lookups | Every login attempt |
| customers | customer_number | Quick customer search | Search operations |
| customers | user_id | Find customer for user | Auth → Customer mapping |
| accounts | account_number | Find account by number | Every transaction |
| accounts | customer_id | Find customer's accounts | List accounts |
| transactions | account_id, created_at | Get transaction history | Statement generation |
| transactions | reference_id | Find transfer pairs | Transfer verification |
| audit_logs | created_at DESC | Recent activity | Admin dashboards |
| audit_logs | actor | User activity audit | Compliance reports |

### 5.2 Composite Indexes

```sql
-- Most queries need account_id + date range
CREATE INDEX idx_transactions_account_date 
ON transactions(account_id, created_at DESC);

-- Better than separate indexes for filtering by both
```

### 5.3 Monitoring Indexes

```sql
-- Check index usage
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- Unused indexes (candidates for removal)
SELECT schemaname, tablename, indexname
FROM pg_stat_user_indexes
WHERE idx_scan = 0
ORDER BY indexname;

-- Index size
SELECT indexname, pg_size_pretty(pg_relation_size(indexrelid))
FROM pg_stat_user_indexes
ORDER BY pg_relation_size(indexrelid) DESC;
```

---

## 6. Constraints & Validation

### 6.1 Database Constraints

```sql
-- Email must be unique
ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);

-- Account balance never negative
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_positive_balance 
    CHECK (balance >= 0);

-- Currency must be valid ISO 4217
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_valid_currency 
    CHECK (currency ~ '^[A-Z]{3}$');

-- Customer number unique
ALTER TABLE customers ADD CONSTRAINT uq_customers_number 
    UNIQUE (customer_number);

-- Cannot delete account with active status
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_status 
    CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED'));

-- Transaction amount always positive
ALTER TABLE transactions ADD CONSTRAINT chk_transactions_positive_amount 
    CHECK (amount > 0);
```

### 6.2 Foreign Key Constraints

```sql
-- Customer references User (1:1)
ALTER TABLE customers ADD CONSTRAINT fk_customers_user_id 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Account references Customer (1:N)
ALTER TABLE accounts ADD CONSTRAINT fk_accounts_customer_id 
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT;

-- Transaction references Account (1:N)
ALTER TABLE transactions ADD CONSTRAINT fk_transactions_account_id 
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT;
```

---

## 7. Transaction Strategy & Concurrency Control

### 7.1 Optimistic Locking

Used for frequently accessed data (accounts):

```java
@Entity
@Table(name = "accounts")
public class Account {
    
    @Version
    private Long version;  // Automatically managed by JPA
    
    @Column(name = "balance")
    private BigDecimal balance;
}

// When updating:
// If version doesn't match → OptimisticLockException
// If version matches → Transaction succeeds, version incremented
```

### 7.2 Transaction Isolation Levels

```properties
# application.properties
spring.jpa.properties.hibernate.connection.isolation=2
# Level 2 = READ_COMMITTED (default, good for banking)
# Level 3 = REPEATABLE_READ (higher consistency, slight performance hit)
```

### 7.3 Transactional Operations

```java
@Transactional(
    propagation = Propagation.REQUIRED,
    isolation = Isolation.READ_COMMITTED,
    rollbackFor = Exception.class
)
public void transfer(String sourceId, String destId, BigDecimal amount) {
    // All or nothing
    // If any step fails → entire transaction rolls back
    // Database returns to pre-transfer state
}
```

---

## 8. Backup & Recovery

### 8.1 Backup Strategy

```bash
# Full backup (daily)
pg_dump -U banking_user -d banking_api -F c > /backups/banking_api_$(date +%Y%m%d).dump

# Incremental backup (WAL archiving)
# Configure postgresql.conf for WAL archiving

# Compressed backup
pg_dump -U banking_user -d banking_api | gzip > banking_api_backup.sql.gz
```

### 8.2 Recovery Procedure

```bash
# Restore from backup
pg_restore -U banking_user -d banking_api /backups/banking_api_20260804.dump

# Restore from SQL dump
psql -U banking_user -d banking_api < banking_api_backup.sql
```

### 8.3 Point-in-Time Recovery (PITR)

Enabled via WAL archiving for recovery to specific point in time.

---

## 9. Flyway Migration Strategy

### 9.1 Migration Approach

- **Versioned Migrations:** V1, V2, V3... (must be in order)
- **Naming:** `V{VERSION}__{DESCRIPTION}.sql`
- **Repeatable:** `R__{DESCRIPTION}.sql` (views, functions)
- **Baseline:** V0 for existing schemas

### 9.2 Safety Guidelines

1. **Always reversible** (if possible)
2. **Backward compatible** (don't break running app)
3. **Tested in dev/staging** before production
4. **Atomic** (single transaction)
5. **Documented** with comments

### 9.3 Example Migrations

See `/src/main/resources/db/migration/` for complete set.

---

## 10. Security & Data Protection

### 10.1 Column-Level Security

```sql
-- Sensitive columns encrypted (application-level)
-- password_hash never selected unnecessarily
-- refresh_tokens.token never logged
```

### 10.2 Row-Level Security (RLS)

```sql
-- Optional: PostgreSQL RLS for multi-tenancy
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;

CREATE POLICY accounts_isolation ON accounts
USING (customer_id = current_user_id());
```

### 10.3 Audit Triggers (Optional)

```sql
CREATE OR REPLACE FUNCTION audit_account_change()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO audit_logs (actor, action, old_value, new_value)
    VALUES (current_user, 'UPDATE_ACCOUNT', row_to_json(OLD), row_to_json(NEW));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER account_audit_trigger
AFTER UPDATE ON accounts
FOR EACH ROW
EXECUTE FUNCTION audit_account_change();
```

---

## 11. Performance Tuning

### 11.1 Connection Pooling

```properties
# HikariCP configuration
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
```

### 11.2 Query Optimization

```sql
-- Enable query statistics
CREATE EXTENSION pg_stat_statements;

-- Find slow queries
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
WHERE mean_exec_time > 100
ORDER BY mean_exec_time DESC;
```

### 11.3 Vacuuming & Maintenance

```sql
-- Automatic vacuuming enabled by default
-- Manual vacuum if needed:
VACUUM ANALYZE transactions;
```

---

## 12. Monitoring & Observability

### 12.1 Database Metrics to Monitor

- Query latency (p99, p95)
- Connection pool utilization
- Slow query log
- Index hit rate
- Table bloat

### 12.2 Prometheus Metrics

```properties
# Expose metrics
spring.jpa.properties.hibernate.generate_statistics=true
management.endpoints.web.exposure.include=metrics,prometheus
```

---

## 13. Capacity Planning

### 13.1 Storage Estimation

```
Transactions table growth:
- Assume 1000 transactions/day
- 365,000 transactions/year
- 365,000 * 300 bytes ≈ 110 MB/year
- 10 years = 1.1 GB (manageable)

With indexes:
- 1.5x-2x storage
- 2-2.2 GB for 10 years
```

### 13.2 Connection Pool Sizing

```
Typical: core_size = (cpu_count * 2) + disk_spindle_count
Banking API: 8 CPUs = 16-20 connections recommended
```

---

## 14. Future Extensions

1. **Multi-Currency Support** - Exchange rate table, daily rates
2. **Scheduled Payments** - Recurring transactions
3. **Beneficiaries** - Saved payment recipients
4. **Fraud Detection** - ML model scoring on transactions
5. **Virtual Accounts** - Temporary accounts for specific purposes
6. **Event Sourcing** - Immutable event log
7. **Data Warehousing** - Analytics database with denormalization

---

## 15. Quick Reference - SQL Cheat Sheet

```sql
-- Create new user and customer
BEGIN;
INSERT INTO users (email, password_hash, enabled) 
VALUES ('newuser@example.com', '$2a$10$...', true);

INSERT INTO customers (user_id, customer_number, full_name)
VALUES (
    (SELECT id FROM users WHERE email = 'newuser@example.com'),
    'CUST-000123',
    'New User'
);
COMMIT;

-- Create account for customer
INSERT INTO accounts (customer_id, account_number, account_type, currency, balance)
VALUES (
    (SELECT id FROM customers WHERE customer_number = 'CUST-000123'),
    'ACC-000001',
    'SAVINGS',
    'USD',
    5000.00
);

-- Record deposit transaction
INSERT INTO transactions (account_id, transaction_type, amount, balance_after, description)
VALUES (
    (SELECT id FROM accounts WHERE account_number = 'ACC-000001'),
    'DEPOSIT',
    1000.00,
    6000.00,
    'Cash deposit'
);

-- Transfer between accounts
BEGIN;
-- Debit source
UPDATE accounts SET balance = balance - 500, version = version + 1 
WHERE id = 'source-uuid' AND version = 5;

INSERT INTO transactions (account_id, reference_id, transaction_type, amount, balance_after)
VALUES ('source-uuid', 'transfer-ref-123', 'DEBIT', 500, 4500);

-- Credit destination
UPDATE accounts SET balance = balance + 500, version = version + 1 
WHERE id = 'dest-uuid' AND version = 3;

INSERT INTO transactions (account_id, reference_id, transaction_type, amount, balance_after)
VALUES ('dest-uuid', 'transfer-ref-123', 'CREDIT', 500, 5500);

COMMIT;
```

---

**Status:** ✅ Comprehensive design ready for implementation  
**Last Updated:** 2026-08-04
