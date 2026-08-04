# Development Setup Guide - Banking API

**Version:** 1.0.0  
**Date:** 2026-08-04  
**Target Developers:** Junior to Senior Backend Engineers

---

## 1. Quick Start (5 Minutes)

### Prerequisites
- Java 21 installed
- Maven 3.9+
- Git

### Steps

```bash
# 1. Clone
git clone https://github.com/your-org/banking-api.git
cd banking-api

# 2. Create local environment
cp .env.example .env.local

# 3. Start dependencies (Docker required)
docker-compose -f docker-compose.dev.yml up -d

# 4. Build and run
mvn clean spring-boot:run

# 5. Verify
curl http://localhost:8080/api/v1/health

# 6. Access Swagger
open http://localhost:8080/swagger-ui.html
```

---

## 2. IDE Setup

### 2.1 IntelliJ IDEA (Recommended)

#### Installation
1. Download from https://www.jetbrains.com/idea/download/
2. Install (Community or Ultimate edition both work)

#### Configuration
```
File → Settings → Build, Execution, Deployment
  ├── Java Compiler
  │   ├── Target bytecode version: 21
  │   └── Project: Set to 21
  ├── Maven
  │   ├── Maven home path: Auto-detected or /usr/local/opt/maven
  │   └── User settings file: ~/.m2/settings.xml
  └── Gradle (if applicable)
      └── Same JDK version
```

#### Useful Plugins
- Install via `File → Settings → Plugins → Marketplace`:
  - **Spring Assistant** - Spring configuration support
  - **Lombok** - Automatic getter/setter generation
  - **Database Tools and SQL** - Built-in database UI
  - **Docker** - Docker & Compose integration
  - **Git Toolbox** - Git workflow improvements
  - **CheckStyle-IDEA** - Code quality checks
  - **Sonar Lint** - Static analysis

#### Project Import
```
File → Open → banking-api/pom.xml → Open as Project
```

#### Run Configurations
1. Create new configuration: `Run → Edit Configurations → + → Spring Boot`
2. Set:
   - **Name:** `BankingApplication`
   - **Main class:** `com.company.banking.BankingApplication`
   - **Program arguments:** `--spring.profiles.active=dev`
   - **VM options:** `-Xmx1024m -XX:+UseG1GC`
3. Click `Apply` and `OK`

#### Debugging
- Set breakpoints by clicking line numbers
- Use `Debug → Debug 'BankingApplication'` to run with debugger
- Step through code with `F7` (step in), `F8` (step over), `Shift+F8` (step out)

### 2.2 VS Code

#### Extensions
```
code --install-extension redhat.java
code --install-extension vscjava.vscode-java-test
code --install-extension vscjava.vscode-maven
code --install-extension rangav.vscode-thunder-client
code --install-extension vmware.vscode-boot-dev-pack
```

#### Settings (`.vscode/settings.json`)
```json
{
  "java.home": "/path/to/java21",
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-21",
      "path": "/path/to/java21"
    }
  ],
  "maven.executable.path": "/usr/local/bin/mvn",
  "[java]": {
    "editor.defaultFormatter": "redhat.java",
    "editor.formatOnSave": true
  }
}
```

#### Debug Configuration (`.vscode/launch.json`)
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Spring Boot App",
      "request": "launch",
      "cwd": "${workspaceFolder}",
      "mainClass": "com.company.banking.BankingApplication",
      "projectName": "banking-api",
      "args": "--spring.profiles.active=dev",
      "console": "integratedTerminal"
    }
  ]
}
```

### 2.3 Eclipse

#### Configuration
1. **Window → Preferences → Java**
   - Compiler: Set to 21
   - Installed JREs: Add Java 21
2. **Window → Preferences → Maven**
   - Discovery: Enable Maven plugin connector discovery
3. **Import → Existing Maven Projects → Select banking-api**

---

## 3. Project Structure Navigation

```
banking-api/
├── .github/
│   └── workflows/              ← CI/CD pipelines
├── docker/                     ← Docker configs
├── docs/                       ← Additional documentation
├── scripts/                    ← Utility scripts
├── src/
│   ├── main/
│   │   ├── java/com/company/banking/
│   │   │   ├── auth/           ← Authentication feature
│   │   │   ├── customer/       ← Customer management
│   │   │   ├── account/        ← Account operations
│   │   │   ├── transaction/    ← Transaction handling
│   │   │   ├── common/         ← Shared utilities
│   │   │   ├── config/         ← Spring configuration
│   │   │   ├── security/       ← Security configs
│   │   │   └── BankingApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── db/migration/   ← Flyway migrations
│   │       └── db/testdata/    ← Test fixtures
│   └── test/
│       ├── java/...            ← Unit & Integration tests
│       └── resources/
├── pom.xml                     ← Maven configuration
├── docker-compose.dev.yml      ← Dev services
├── docker-compose.yml          ← Prod services
├── .env.example               ← Environment template
└── README.md
```

### 3.1 Navigating by Feature

To work on **Account** feature:
1. Open `src/main/java/com/company/banking/account/`
2. Structure:
   - `presentation/` - REST controllers
   - `application/` - Use cases and DTOs
   - `domain/` - Core business logic
   - `infrastructure/` - Database implementations

---

## 4. Build & Run Commands

### 4.1 Maven Commands

```bash
# Build project
mvn clean build

# Skip tests
mvn clean build -DskipTests

# Run tests
mvn test

# Run integration tests
mvn verify

# Run specific test
mvn test -Dtest=AccountServiceTest

# Generate project reports
mvn site

# Install to local repo
mvn install

# Check for dependency updates
mvn versions:display-dependency-updates

# Check for plugin updates
mvn versions:display-plugin-updates
```

### 4.2 Running Application

```bash
# Using Maven
mvn spring-boot:run

# Using IDE debug mode (recommended for development)
# Right-click BankingApplication.java → Debug As → Java Application

# With specific profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# With custom JVM options
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx1024m -Xms512m"
```

### 4.3 Database Commands

```bash
# Run Flyway migrations
mvn flyway:migrate

# Check migration status
mvn flyway:info

# Validate migrations without running
mvn flyway:validate

# Clean database (WARNING: deletes data)
mvn flyway:clean
```

---

## 5. Code Development Workflow

### 5.1 Branching Strategy (Git Flow)

```bash
# Create feature branch
git checkout -b feature/FR-01-customer-registration

# Create bugfix branch
git checkout -b bugfix/BUG-123-transaction-rollback

# Create release branch
git checkout -b release/1.0.0

# Push to remote
git push -u origin feature/FR-01-customer-registration
```

### 5.2 Feature Development Checklist

1. **Create feature branch** from `develop`
2. **Implement business logic** in domain layer
3. **Add service layer** in application layer
4. **Create REST controller** in presentation layer
5. **Write unit tests** (TDD approach recommended)
6. **Write integration tests** for critical paths
7. **Update API documentation** (Swagger annotations)
8. **Update database migrations** if needed
9. **Commit with semantic message**:
   ```
   feat(account): add account freeze functionality
   
   - Add freeze() method to Account entity
   - Add AccountFreezeUseCase
   - Add AccountFreezeController endpoint
   - Add integration tests
   ```
10. **Push and create Pull Request**
11. **Wait for CI/CD and code review**

### 5.3 Coding Standards

#### Java Naming Conventions
```java
// Classes: PascalCase
public class AccountService { }
public class CreateAccountCommand { }

// Methods: camelCase
public Account createAccount(CreateAccountCommand cmd) { }
public void freezeAccount(String accountId) { }

// Constants: UPPER_SNAKE_CASE
private static final int MAX_ACCOUNTS_PER_CUSTOMER = 10;
private static final String ACCOUNT_NUMBER_PREFIX = "ACC";

// Variables: camelCase
String accountNumber;
List<Transaction> transactions;
boolean isActive;
```

#### Code Organization
```java
public class AccountService {
    
    // 1. Logger
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    
    // 2. Dependencies
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;
    
    // 3. Constructor
    public AccountService(AccountRepository repo, TransactionService txnService) {
        this.accountRepository = repo;
        this.transactionService = txnService;
    }
    
    // 4. Public methods (use case entry points)
    public Account createAccount(CreateAccountCommand cmd) { }
    
    // 5. Private helper methods
    private void validateAccountCreation(CreateAccountCommand cmd) { }
    
    // 6. Exception handlers (at end)
    private void handleOptimisticLockException(Exception e) { }
}
```

#### Exception Handling
```java
// Use checked exceptions for business logic
public class InsufficientBalanceException extends BusinessException {
    public InsufficientBalanceException(BigDecimal required, BigDecimal available) {
        super("INSUFFICIENT_BALANCE",
              String.format("Required: %s, Available: %s", required, available));
    }
}

// Usage
try {
    account.withdraw(1000.00);
} catch (InsufficientBalanceException e) {
    logger.warn("Withdrawal failed: {}", e.getMessage());
    throw new BusinessException("INSUFFICIENT_BALANCE", e.getMessage());
}
```

### 5.4 Testing Best Practices

```java
// Test naming: what → expected result
@Test
@DisplayName("should reject withdrawal when balance insufficient")
void shouldRejectWithdrawalWhenBalanceInsufficient() {
    // Arrange: setup test data
    Account account = createAccountWithBalance(100.00);
    
    // Act: perform action
    assertThatThrownBy(() -> account.withdraw(500.00))
        // Assert: verify expectations
        .isInstanceOf(InsufficientBalanceException.class);
}

// Use Nested classes for organization
@Nested
@DisplayName("Transfer Money")
class TransferMoneyTests {
    
    @Test
    void shouldTransferSuccessfully() { }
    
    @Test
    void shouldFailWithSameAccount() { }
}
```

---

## 6. Database Development

### 6.1 Creating New Migration

```bash
# Create migration file
touch src/main/resources/db/migration/V{version}__description.sql

# Example filename
# V9__Create_beneficiaries_table.sql
```

### 6.2 Migration Template

```sql
-- V9__Create_beneficiaries_table.sql
-- Date: 2026-08-04
-- Description: Add beneficiaries table for transfer recipients

BEGIN;

-- Create beneficiaries table
CREATE TABLE IF NOT EXISTS beneficiaries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    account_number VARCHAR(20) NOT NULL,
    account_holder_name VARCHAR(150) NOT NULL,
    bank_code VARCHAR(10),
    is_verified BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(customer_id, account_number)
);

-- Create indexes
CREATE INDEX idx_beneficiaries_customer_id ON beneficiaries(customer_id);

-- Add constraint
ALTER TABLE beneficiaries ADD CONSTRAINT chk_beneficiary_not_own_account
    CHECK (account_id != id);

COMMIT;
```

### 6.2 Database Tools in IDE

#### IntelliJ IDEA Database
1. Open: `View → Tool Windows → Database`
2. Click `+` → PostgreSQL
3. Configure:
   - Host: localhost
   - Port: 5432
   - User: banking_user
   - Password: SecurePassword123!
   - Database: banking_api
4. Test connection
5. Execute SQL queries directly in IDE

---

## 7. API Testing During Development

### 7.1 Using Swagger UI

```
http://localhost:8080/swagger-ui.html
```

- Interactive API documentation
- Try endpoints directly
- See request/response schemas
- Test with real data

### 7.2 Using cURL

```bash
# Register user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!",
    "fullName": "John Doe"
  }'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!"
  }'

# Use token in request
curl -X GET http://localhost:8080/api/v1/customers \
  -H "Authorization: Bearer {token}"
```

### 7.3 Using Postman

1. Import collection: `Postman collection file` (to be provided)
2. Set environment variables:
   - `base_url`: http://localhost:8080/api/v1
   - `token`: (obtained from login response)
3. Run requests

### 7.4 Using HTTPie (Simpler than cURL)

```bash
# Install
brew install httpie

# Register
http POST localhost:8080/api/v1/auth/register \
  email=user@example.com \
  password=SecurePass123! \
  fullName="John Doe"

# Login and save token
TOKEN=$(http POST localhost:8080/api/v1/auth/login \
  email=user@example.com \
  password=SecurePass123! | jq -r '.accessToken')

# Use token
http GET localhost:8080/api/v1/customers \
  "Authorization: Bearer $TOKEN"
```

---

## 8. Debugging Tips

### 8.1 Enable Debug Logging

File: `application-dev.properties`

```properties
# Package-specific debug
logging.level.com.company.banking=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Save logs to file
logging.file.name=logs/application.log
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

### 8.2 Add Debug Points

IntelliJ IDEA:
1. Click on line number to add breakpoint
2. Right-click breakpoint → Edit Breakpoint
3. Set condition: `accountId.equals("specific-id")`
4. Set "Evaluate and log" to print variables

### 8.3 Common Debugging Scenarios

#### Transaction Not Rolling Back
```java
// Add logging
@Transactional
public void transfer(...) {
    try {
        logger.debug("Starting transfer from {} to {}", sourceId, destId);
        // ... operation
        logger.debug("Transfer completed");
    } catch (Exception e) {
        logger.error("Transfer failed, rolling back", e);
        throw e;
    }
}
```

#### Optimistic Lock Exception
```java
// Check version mismatch
Account account = repo.findById(id).orElse(null);
logger.debug("Account version: {}", account.getVersion());

// Reload fresh
Account fresh = repo.findById(id).orElse(null);
logger.debug("Fresh version: {}", fresh.getVersion());
```

---

## 9. Common Development Scenarios

### 9.1 Adding New REST Endpoint

1. **Create Use Case** (Application layer)
   ```java
   public class FreezeAccountUseCase {
       public void execute(String accountId) { }
   }
   ```

2. **Create Controller** (Presentation layer)
   ```java
   @PatchMapping("/{id}/freeze")
   public ResponseEntity<AccountResponse> freeze(@PathVariable String id) { }
   ```

3. **Add Swagger** annotations
   ```java
   @Operation(summary = "Freeze account")
   @ApiResponse(responseCode = "200", description = "Account frozen")
   ```

4. **Write Tests**
   ```java
   @Test
   void shouldFreezeAccount() { }
   ```

### 9.2 Adding New Database Column

1. **Create migration**
   ```sql
   ALTER TABLE accounts ADD COLUMN is_locked BOOLEAN DEFAULT false;
   ```

2. **Update JPA entity**
   ```java
   @Column(name = "is_locked")
   private boolean locked;
   ```

3. **Update domain entity** (if applicable)

4. **Update DTO/Response** if API exposed

5. **Run migration**
   ```bash
   mvn flyway:migrate
   ```

### 9.3 Debugging Concurrent Requests

Use thread names in logging:
```java
logger.debug("[Thread: {}] Processing transaction", Thread.currentThread().getName());
```

Check Actuator threads:
```bash
curl http://localhost:8080/actuator/threaddump | jq
```

---

## 10. Productivity Tips

### 10.1 IntelliJ IDEA Shortcuts

| Shortcut | Action |
|----------|--------|
| `Cmd/Ctrl+Shift+A` | Find action |
| `Cmd/Ctrl+P` | Show parameter info |
| `Cmd/Ctrl+B` | Go to definition |
| `Cmd/Ctrl+F12` | Show file structure |
| `Cmd/Ctrl+/` | Toggle line comment |
| `Cmd/Ctrl+D` | Duplicate line |
| `Cmd/Ctrl+K` | Commit changes |
| `Cmd/Ctrl+T` | Update from VCS |
| `F7` | Step into debugger |
| `F8` | Step over debugger |

### 10.2 Live Templates

Type and press Tab:
- `psvm` → `public static void main(String[] args) {}`
- `sout` → `System.out.println();`
- `ifn` → `if (... != null) {}`

### 10.3 Code Generation

- `Cmd/Ctrl+N` → Generate class
- `Cmd/Ctrl+O` → Generate getters/setters
- `Cmd/Ctrl+I` → Implement methods
- `Cmd/Ctrl+Alt+T` → Surround with try/catch

---

## 11. Troubleshooting Development Issues

### Issue: Port 8080 Already in Use
```bash
# Find process
lsof -i :8080

# Kill process
kill -9 <PID>

# Or change port
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

### Issue: Database Connection Refused
```bash
# Start PostgreSQL
docker-compose -f docker-compose.dev.yml up -d postgres

# Verify
docker-compose -f docker-compose.dev.yml ps
```

### Issue: Migrations Not Applied
```bash
# Clean and remigrate
mvn flyway:clean flyway:migrate -DskipTests

# Check status
mvn flyway:info
```

### Issue: Tests Failing Locally but Passing in CI
- Ensure same Java version (use `java -version`)
- Clear Maven cache: `rm -rf ~/.m2/repository/`
- Rebuild: `mvn clean install`

---

## 12. Resources & References

- **Spring Boot Documentation**: https://spring.io/projects/spring-boot
- **Spring Security**: https://spring.io/projects/spring-security
- **Spring Data JPA**: https://spring.io/projects/spring-data-jpa
- **Clean Architecture**: https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html
- **Domain-Driven Design**: https://www.domainlanguage.com/ddd/
- **Effective Java**: Joshua Bloch - Read this book
- **PostgreSQL Documentation**: https://www.postgresql.org/docs/

---
