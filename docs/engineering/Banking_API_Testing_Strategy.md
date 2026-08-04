# Testing Strategy - Banking API

**Version:** 1.0.0  
**Date:** 2026-08-04  
**Target Coverage:** 80%+ Unit Test Coverage, Critical Path Integration Tests

---

## 1. Testing Philosophy

- **Test Pyramid**: Unit Tests (70%) → Integration Tests (20%) → E2E Tests (10%)
- **FIRST Principle**: Fast, Independent, Repeatable, Self-checking, Timely
- **TDD Ready**: Tests define contracts before implementation
- **Clean Testing**: No test interdependencies, setup/teardown isolation
- **Business-Driven**: Tests verify business rules, not implementation details

---

## 2. Unit Testing Strategy

### 2.1 Scope
- Domain entities and value objects
- Domain services and business rules
- Application services and use cases
- Validators
- Mappers and DTOs

### 2.2 Tools & Framework
- **Framework**: JUnit 5 (Jupiter)
- **Mocking**: Mockito, MockitoExtension
- **Assertions**: AssertJ for fluent assertions
- **Parameterized Tests**: `@ParameterizedTest` for multiple scenarios

### 2.3 Test Structure

```java
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;
    
    @InjectMocks
    private AccountService accountService;
    
    @Nested
    @DisplayName("Create Account")
    class CreateAccountTests {
        
        @Test
        @DisplayName("should successfully create account with valid data")
        void shouldCreateAccount() {
            // Arrange
            CreateAccountCommand command = new CreateAccountCommand(
                customerId, "SAVINGS", "USD", 1000.00
            );
            Account expectedAccount = Account.create(command);
            when(accountRepository.save(any())).thenReturn(expectedAccount);
            
            // Act
            Account result = accountService.createAccount(command);
            
            // Assert
            assertThat(result)
                .isNotNull()
                .extracting(Account::getBalance)
                .isEqualTo(1000.00);
            
            verify(accountRepository, times(1)).save(any());
        }
        
        @Test
        @DisplayName("should fail with negative initial balance")
        void shouldFailWithNegativeBalance() {
            CreateAccountCommand command = new CreateAccountCommand(
                customerId, "SAVINGS", "USD", -100.00
            );
            
            assertThatThrownBy(() -> Account.create(command))
                .isInstanceOf(InvalidAccountBalanceException.class);
        }
        
        @ParameterizedTest
        @ValueSource(doubles = { 0, -50, -1000 })
        @DisplayName("should reject invalid initial balance")
        void shouldRejectInvalidBalance(double balance) {
            CreateAccountCommand command = new CreateAccountCommand(
                customerId, "SAVINGS", "USD", balance
            );
            
            assertThatThrownBy(() -> Account.create(command))
                .isInstanceOf(InvalidAccountBalanceException.class);
        }
    }
    
    @Nested
    @DisplayName("Withdraw Money")
    class WithdrawMoneyTests {
        
        @Test
        @DisplayName("should successfully withdraw valid amount")
        void shouldWithdraw() {
            // Setup
            Account account = Account.create(customerId, "SAVINGS", "USD", 5000.00);
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
            
            // Execute
            WithdrawCommand cmd = new WithdrawCommand(accountId, 1000.00);
            Transaction transaction = accountService.withdraw(cmd);
            
            // Assert
            assertThat(transaction.getAmount()).isEqualTo(1000.00);
            assertThat(account.getBalance()).isEqualTo(4000.00);
            assertThat(transaction.getType()).isEqualTo(TransactionType.WITHDRAW);
        }
        
        @Test
        @DisplayName("should fail with insufficient balance")
        void shouldFailWithInsufficientBalance() {
            Account account = Account.create(customerId, "SAVINGS", "USD", 500.00);
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
            
            WithdrawCommand cmd = new WithdrawCommand(accountId, 1000.00);
            
            assertThatThrownBy(() -> accountService.withdraw(cmd))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Insufficient balance");
        }
        
        @Test
        @DisplayName("should fail on frozen account")
        void shouldFailOnFrozenAccount() {
            Account account = Account.create(customerId, "SAVINGS", "USD", 5000.00);
            account.freeze();
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
            
            WithdrawCommand cmd = new WithdrawCommand(accountId, 1000.00);
            
            assertThatThrownBy(() -> accountService.withdraw(cmd))
                .isInstanceOf(FrozenAccountException.class);
        }
    }
}
```

### 2.4 Coverage Targets by Module

| Module | Target Coverage | Critical Paths |
|--------|-----------------|-----------------|
| Authentication | 85% | Register, Login, Token Refresh |
| Customer | 80% | CRUD, Soft Delete |
| Account | 90% | Create, Freeze, Close |
| Transactions | 90% | Deposit, Withdraw, Transfer (atomic) |
| Transfer | 95% | Atomic debit/credit, Rollback |
| Dashboard | 75% | Metrics calculation |
| Audit | 70% | Log capture |

---

## 3. Integration Testing Strategy

### 3.1 Scope
- API endpoints with real Spring context
- Database interactions with embedded PostgreSQL
- Transaction boundaries and rollback
- Security and RBAC

### 3.2 Tools
- **Testcontainers**: PostgreSQL, Redis for isolated environments
- **Spring Boot Test**: `@SpringBootTest`, `@WebMvcTest`
- **MockMvc**: HTTP layer testing
- **RestAssured**: HTTP assertions (alternative)

### 3.3 Test Structure

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AccountControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:17")
    )
    .withDatabaseName("test_banking")
    .withUsername("test")
    .withPassword("test");
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private TestDataBuilder testDataBuilder;
    
    @Test
    @DisplayName("should create account successfully")
    @WithMockUser(roles = "ADMIN")
    void shouldCreateAccount() throws Exception {
        Customer customer = testDataBuilder.createCustomer();
        
        CreateAccountRequest request = new CreateAccountRequest(
            customer.getId(),
            "SAVINGS",
            "USD",
            5000.00
        );
        
        mockMvc.perform(post("/api/v1/accounts")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accountNumber").isNotEmpty())
            .andExpect(jsonPath("$.balance").value(5000.00));
    }
    
    @Test
    @DisplayName("should reject withdraw with insufficient balance")
    @WithMockUser(roles = "CUSTOMER")
    void shouldRejectWithdrawWithInsufficientBalance() throws Exception {
        Account account = testDataBuilder.createAccountWithBalance(100.00);
        
        WithdrawRequest request = new WithdrawRequest(
            account.getId(),
            500.00,
            "ATM withdrawal"
        );
        
        mockMvc.perform(post("/api/v1/transactions/withdraw")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .with(csrf()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"));
    }
    
    @Test
    @DisplayName("should atomically transfer money between accounts")
    @WithMockUser(roles = "CUSTOMER")
    void shouldAtomicallyTransferMoney() throws Exception {
        Account source = testDataBuilder.createAccountWithBalance(5000.00);
        Account destination = testDataBuilder.createAccountWithBalance(0.00);
        
        TransferRequest request = new TransferRequest(
            source.getId(),
            destination.getId(),
            500.00,
            "Transfer to friend"
        );
        
        mockMvc.perform(post("/api/v1/transactions/transfer")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.referenceId").isNotEmpty())
            .andExpect(jsonPath("$.sourceTransaction.amount").value(500.00))
            .andExpect(jsonPath("$.destinationTransaction.amount").value(500.00));
        
        // Verify final balances
        mockMvc.perform(get("/api/v1/accounts/{id}", source.getId())
            .with(csrf()))
            .andExpect(jsonPath("$.balance").value(4500.00));
            
        mockMvc.perform(get("/api/v1/accounts/{id}", destination.getId())
            .with(csrf()))
            .andExpect(jsonPath("$.balance").value(500.00));
    }
    
    @Test
    @DisplayName("should handle optimistic lock exception on concurrent transfer")
    @WithMockUser(roles = "CUSTOMER")
    void shouldHandleOptimisticLock() throws Exception {
        Account account = testDataBuilder.createAccountWithBalance(5000.00);
        account.setVersion(1);
        
        // Simulate concurrent modification
        testDataBuilder.updateAccountVersion(account.getId(), 2);
        
        TransferRequest request = new TransferRequest(
            account.getId(),
            destinationId,
            500.00
        );
        
        mockMvc.perform(post("/api/v1/transactions/transfer")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .with(csrf()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("OPTIMISTIC_LOCK_EXCEPTION"));
    }
}
```

### 3.4 Critical Integration Test Scenarios

#### Authentication Flow
- Register → Login → Refresh Token → Logout
- Invalid credentials rejection
- Token expiration handling
- RBAC enforcement

#### Deposit/Withdraw/Transfer
- Successful deposit increases balance
- Successful withdraw decreases balance
- Concurrent transfers handle optimistic locking
- Rollback on failure (atomic)
- Transaction history records all operations

#### Account Lifecycle
- Freeze account → cannot transact
- Close account → cannot transact
- Soft delete customer → still queryable with soft delete flag

#### Dashboard Metrics
- Metrics calculation accuracy
- Performance under load

---

## 4. End-to-End Testing Strategy

### 4.1 Scope
- Complete user journeys
- Real browser (Selenium, Playwright) OR API chains
- API test automation via Postman/RestAssured

### 4.2 Test Scenarios

```gherkin
# E2E Test Scenarios

Feature: Banking Operations
  Scenario: Customer registers, deposits, transfers, and views history
    Given a new customer registers with email "test@example.com"
    When customer logs in successfully
    Then customer receives JWT access token
    
    When customer creates a savings account with $5000 initial balance
    Then account is created with correct balance
    And transaction history records the deposit
    
    When customer deposits $1000 additional funds
    Then balance is now $6000
    And deposit transaction appears in history
    
    When customer transfers $500 to another account
    Then source balance is $5500
    And destination balance is $500
    And transfer reference ID is recorded
    
    When customer views transaction history with filters
    Then history shows all transactions with correct amounts
    And pagination works correctly
    And sorting by date works
    
    When admin views dashboard
    Then dashboard shows correct total customers
    And total balance reflects all transactions
```

### 4.3 Tools
- **RestAssured** for API chain testing
- **Testcontainers** for isolated environments
- **WireMock** for mocking external services (if any)

---

## 5. Performance Testing

### 5.1 Goals
- 95% of requests complete within 500ms (under normal load)
- Support 100+ concurrent users
- Database connection pooling optimized

### 5.2 Test Plan
- Load test deposit endpoint: 1000 requests/min
- Load test transfer endpoint: 500 requests/min
- Identify bottlenecks and optimize

### 5.3 Tools
- Apache JMeter or Gatling for load tests

---

## 6. Security Testing

### 6.1 Unit Tests
- Password validation (min 8 chars, complexity)
- BCrypt hashing verification
- JWT validation

### 6.2 Integration Tests
- RBAC enforcement (customer cannot access admin endpoints)
- SQL injection prevention (parameterized queries)
- XSS prevention via @Valid and sanitization
- CSRF protection

### 6.3 Checklist
- [ ] Authorization header required for protected endpoints
- [ ] Expired tokens rejected
- [ ] Customer cannot access other customer's accounts
- [ ] Admin cannot transfer customer funds
- [ ] Input validation prevents malicious payloads
- [ ] Rate limiting blocks excessive requests

---

## 7. Test Data Management

### 7.1 Test Data Builder Pattern

```java
@Component
public class TestDataBuilder {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    public Customer createCustomer() {
        User user = User.builder()
            .email("test-" + UUID.randomUUID() + "@example.com")
            .passwordHash(BCryptPasswordEncoder.encode("Test@123"))
            .enabled(true)
            .build();
        
        Customer customer = Customer.builder()
            .customerNumber("CUST-" + System.currentTimeMillis())
            .fullName("Test Customer")
            .phone("+1-555-0123")
            .address("Test Address")
            .status(CustomerStatus.ACTIVE)
            .user(user)
            .build();
        
        return customerRepository.save(customer);
    }
    
    public Account createAccountWithBalance(double balance) {
        Customer customer = createCustomer();
        Account account = Account.create(
            customer.getId(),
            "SAVINGS",
            "USD",
            balance
        );
        return accountRepository.save(account);
    }
}
```

### 7.2 Test Database Isolation
- Each test runs in transaction that rolls back after
- Testcontainers provides fresh database per test suite
- Parallel test execution safe with isolated data

---

## 8. CI/CD Integration

### 8.1 GitHub Actions Pipeline

```yaml
name: Test Suite

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:17
        env:
          POSTGRES_DB: test_banking
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
    
    steps:
      - uses: actions/checkout@v3
      
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      
      - name: Run Unit Tests
        run: mvn test -DskipIntegrationTests
      
      - name: Run Integration Tests
        run: mvn verify -Dskip.unit.tests=true
      
      - name: Check Code Coverage
        run: mvn jacoco:report
      
      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v3
```

---

## 9. Code Coverage Requirements

### 9.1 Thresholds

| Category | Minimum |
|----------|---------|
| Line Coverage | 80% |
| Branch Coverage | 75% |
| Cyclomatic Complexity | <= 10 per method |

### 9.2 Excluded from Coverage
- Spring configuration classes
- DTO/Entity boilerplate (Lombok)
- Main application class
- Exception declarations

### 9.3 Coverage Tools
- **JaCoCo** for code coverage reporting
- GitHub Actions status checks enforce thresholds

---

## 10. Test Execution

### 10.1 Local Development
```bash
# Unit tests only
mvn test

# Integration tests only
mvn verify -Dskip.unit.tests=true

# All tests with coverage
mvn clean verify

# Run specific test class
mvn test -Dtest=AccountServiceTest

# Run with specific tag
mvn test -Dgroups=unit

# Parallel test execution
mvn test -T 1C
```

### 10.2 Test Organization

```
src/test/java/com/company/banking/
├── auth/
│   ├── AuthServiceTest.java
│   ├── JwtProviderTest.java
│   └── AuthControllerIntegrationTest.java
├── account/
│   ├── AccountServiceTest.java
│   ├── AccountControllerIntegrationTest.java
│   └── fixture/
│       └── AccountTestFixture.java
├── transaction/
│   ├── TransferServiceTest.java
│   └── TransferIntegrationTest.java
└── integration/
    └── e2e/
        └── BankingJourneyTest.java
```

---

## 11. Test Review Checklist

### Before Committing
- [ ] All tests pass locally
- [ ] Code coverage >= 80%
- [ ] No flaky tests (run 3 times)
- [ ] Test names are descriptive
- [ ] Arrange-Act-Assert pattern followed
- [ ] No test interdependencies
- [ ] Mock objects used correctly
- [ ] Edge cases covered

### Code Review
- [ ] Tests verify behavior, not implementation
- [ ] No duplicate test code (use fixtures)
- [ ] Performance tests documented
- [ ] Security tests complete

---

## 12. Test Reporting

### 12.1 Reports Generated
- JUnit XML report for CI/CD
- JaCoCo coverage report (HTML)
- Surefire test report
- Failsafe integration test report

### 12.2 Accessing Reports
```bash
# After running: mvn clean verify

# Open coverage report
open target/site/jacoco/index.html

# View test results
open target/surefire-reports/index.html
```

---

## 13. Continuous Improvement

### 13.1 Metrics to Track
- Test pass rate
- Code coverage trend
- Test execution time
- Failed tests by category

### 13.2 Regular Reviews
- Monthly: Review flaky tests
- Quarterly: Assess coverage gaps
- Annually: Architecture fit review

---
