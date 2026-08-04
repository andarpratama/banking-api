# Security & Performance Guidelines - Banking API

**Version:** 1.0.0  
**Date:** 2026-08-04  
**Compliance:** OWASP Top 10, Banking Security Standards

---

## 1. Security Guidelines

### 1.1 OWASP Top 10 Compliance

#### 1. Broken Access Control

**Risk:** Users access unauthorized resources

**Mitigation:**
```java
// ✓ Correct: Verify user owns resource
@GetMapping("/accounts/{id}")
public ResponseEntity<AccountResponse> getAccount(@PathVariable String id) {
    Account account = accountService.getAccount(id);
    authorizationService.verifyOwner(id, getCurrentUserId());
    return ResponseEntity.ok(toResponse(account));
}

// ✗ Wrong: No ownership check
@GetMapping("/accounts/{id}")
public ResponseEntity<AccountResponse> getAccount(@PathVariable String id) {
    return ResponseEntity.ok(toResponse(accountService.getAccount(id)));
}
```

**Implementation:**
- Add `@PreAuthorize` annotations
- Check resource ownership in service layer
- Use role-based access control
- Log authorization failures

#### 2. Cryptographic Failures

**Risk:** Sensitive data exposed or tampered

**Mitigation:**
```java
// ✓ Correct: Use strong encryption
@Configuration
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Cost factor 12+
    }
    
    @Bean
    public JwtProvider jwtProvider(JwtProperties props) {
        return new JwtProvider(
            props.getSecret(),  // Min 32 characters
            props.getExpiration()
        );
    }
}

// Database-level encryption
@Column(name = "password_hash")
@Encrypted  // Use @Encrypted for sensitive fields
private String passwordHash;
```

**Checklist:**
- [ ] HTTPS/TLS enabled in production
- [ ] JWT secret >= 32 characters, randomly generated
- [ ] Database connections over SSL
- [ ] Sensitive data never logged
- [ ] Password hash using BCrypt cost >= 10

#### 3. Injection

**Risk:** SQL/NoSQL injection, command injection

**Mitigation:**
```java
// ✓ Correct: Parameterized queries (Spring Data JPA default)
@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findByAccountNumber(String number);  // Parameterized
}

// ✓ Correct: Input validation
@PostMapping("/accounts")
public ResponseEntity<?> create(@Valid @RequestBody CreateAccountRequest req) {
    // @Valid triggers @NotBlank, @NotNull, @Min annotations
    accountService.create(req);
}

// ✗ Wrong: String concatenation in queries
String query = "SELECT * FROM accounts WHERE id = '" + id + "'";

// ✗ Wrong: No validation
public void transferMoney(String amount) {  // Never validate at API level only
}
```

**Implementation:**
```java
// Use PreparedStatement (or Spring Data which does this)
public List<Transaction> findByDateRange(LocalDate from, LocalDate to) {
    // Spring Data JPA handles parameterization
    return transactionRepository.findByCreatedAtBetween(from, to);
}

// Input validation at DTO level
@Data
public class TransferRequest {
    @NotNull(message = "Source account required")
    private String sourceAccountId;
    
    @NotNull(message = "Destination account required")
    private String destinationAccountId;
    
    @DecimalMin(value = "0.01", message = "Amount must be > 0")
    @DecimalMax(value = "999999.99", message = "Amount too large")
    private BigDecimal amount;
}
```

#### 4. Insecure Design

**Risk:** Missing security controls by design

**Mitigation:**
```java
// ✓ Correct: Rate limiting
@RestController
@RateLimiter(name = "api-limiter")  // Max 100 per minute
public class AuthController {
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }
}

// ✓ Correct: Account lockout
public class LoginAttemptService {
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;
    
    public void recordFailedLogin(String email) {
        if (getFailedAttempts(email) >= MAX_ATTEMPTS) {
            lockAccount(email, LOCK_DURATION_MINUTES);
        }
    }
}

// ✓ Correct: CSRF protection enabled by default
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
        return http.build();
    }
}
```

#### 5. Broken Authentication

**Risk:** Weak authentication mechanisms

**Mitigation:**
```java
// ✓ Correct: Strong password requirements
@Component
public class PasswordValidator {
    
    public void validate(String password) {
        if (password.length() < 8) {
            throw new InvalidPasswordException("Min 8 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new InvalidPasswordException("Must contain uppercase");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new InvalidPasswordException("Must contain lowercase");
        }
        if (!password.matches(".*\\d.*")) {
            throw new InvalidPasswordException("Must contain digit");
        }
        if (!password.matches(".*[!@#$%^&*].*")) {
            throw new InvalidPasswordException("Must contain special character");
        }
    }
}

// ✓ Correct: Secure token refresh
public class RefreshTokenService {
    
    public String refreshAccessToken(String refreshToken) {
        RefreshToken token = findByToken(refreshToken);
        
        if (token.isRevoked() || token.isExpired()) {
            throw new InvalidTokenException("Token invalid or expired");
        }
        
        // Rotate refresh token (invalidate old one)
        token.setRevoked(true);
        save(token);
        
        // Generate new tokens
        RefreshToken newRefreshToken = createNewToken(token.getUser());
        String newAccessToken = jwtProvider.createAccessToken(token.getUser());
        
        return new TokenResponse(newAccessToken, newRefreshToken.getToken());
    }
}

// ✓ Correct: Session management
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.sessionManagement()
            .sessionFixationProtection(SessionFixationProtectionStrategy.MIGRATE_SESSION)
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // Stateless for JWT
            .maximumSessions(1);  // Only one active session per user
        return http.build();
    }
}
```

#### 6. Sensitive Data Exposure

**Risk:** Logging, response exposure of sensitive data

**Mitigation:**
```java
// ✓ Correct: Mask sensitive data in logs
@Slf4j
public class AuthService {
    
    public void login(String email, String password) {
        // NEVER log password
        logger.info("Login attempt for email: {}", maskEmail(email));
        
        // Authenticate
        authenticate(email, password);
        
        logger.info("Login successful for: {}", maskEmail(email));
    }
    
    private String maskEmail(String email) {
        return email.replaceAll("(^[^@]{2})[^@]*([@].*)$", "$1****$2");
        // user@example.com → us****@example.com
    }
}

// ✓ Correct: Never expose internal errors
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        // NEVER: return e.getMessage() - exposes internal details
        logger.error("Unexpected error", e);  // Log full details server-side
        
        return ResponseEntity.status(500).body(new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred"  // Generic message to client
        ));
    }
}

// ✓ Correct: Exclude sensitive fields from response
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private String id;
    private String email;
    private String fullName;
    
    // NEVER include passwordHash
    // @JsonIgnore
    // private String passwordHash;
}

// ✓ Correct: Clear sensitive data from memory
public void logout(String token) {
    RefreshToken refreshToken = findByToken(token);
    refreshToken.setRevoked(true);  // Invalidate
    
    // NEVER store plain tokens in memory
    // Clear from cache
    cache.invalidate("token:" + token);
}
```

#### 7. XML External Entities (XXE)

**Risk:** XML injection attacks

**Mitigation:**
```java
// ✓ Correct: Disable XXE
@Configuration
public class JacksonConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Disable XXE
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(
            XMLConstants.ACCESS_EXTERNAL_DTD, "");
        xmlMapper.configure(
            XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        
        return mapper;
    }
}

// ✓ Correct: Validate XML input
public void processXml(String xmlContent) {
    if (xmlContent.contains("<!DOCTYPE") || 
        xmlContent.contains("<!ENTITY")) {
        throw new InvalidXmlException("External entities not allowed");
    }
    
    // Process XML
}
```

#### 8. Broken Access Control (Already covered in #1)

#### 9. Using Components with Known Vulnerabilities

**Risk:** Outdated dependencies with CVEs

**Mitigation:**
```bash
# Check for vulnerabilities
mvn dependency-check:check

# Update dependencies
mvn versions:display-dependency-updates

# Use dependency management lock file
# pom.xml dependency management section with specific versions
```

Configuration:
```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>8.4.0</version>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>  <!-- Fail on High severity -->
    </configuration>
</plugin>
```

#### 10. Insufficient Logging & Monitoring

**Risk:** Attacks undetected

**Mitigation:**
```java
// ✓ Correct: Audit critical operations
@Slf4j
public class AuditService {
    
    public void auditLogin(String email, String ipAddress, boolean success) {
        logger.info("LOGIN_ATTEMPT email={} ip={} success={}", 
            maskEmail(email), ipAddress, success);
        
        AuditLog log = AuditLog.builder()
            .actor(email)
            .action("LOGIN")
            .status(success ? "SUCCESS" : "FAILURE")
            .ipAddress(ipAddress)
            .timestamp(LocalDateTime.now())
            .build();
        
        auditLogRepository.save(log);
    }
    
    public void auditTransfer(String fromAccount, String toAccount, BigDecimal amount) {
        logger.info("TRANSFER from={} to={} amount={}", 
            maskAccountNumber(fromAccount), 
            maskAccountNumber(toAccount), 
            amount);
        
        AuditLog log = AuditLog.builder()
            .actor(getCurrentUser())
            .action("TRANSFER")
            .endpoint("/transactions/transfer")
            .status("SUCCESS")
            .timestamp(LocalDateTime.now())
            .build();
        
        auditLogRepository.save(log);
    }
}

// ✓ Correct: Monitor suspicious patterns
@Component
@Slf4j
public class AnomalyDetector {
    
    public void detectAndAlert(String email, String action) {
        // Detect: Multiple failed login attempts
        int failedAttempts = getFailedLoginAttempts(email);
        if (failedAttempts > 5) {
            alertSecurityTeam("Multiple failed logins: " + email);
            lockAccount(email);
        }
        
        // Detect: Large transfer
        if (action.equals("TRANSFER") && getAmount() > 50000) {
            alertSecurityTeam("Large transfer detected: " + email);
        }
        
        // Detect: Unusual location
        String userLocation = getUserLocation(email);
        String currentLocation = getCurrentLocation();
        if (!isSameRegion(userLocation, currentLocation)) {
            logger.warn("Location change detected for: {}", maskEmail(email));
        }
    }
}
```

---

### 1.2 Banking Security Standards

#### PCI DSS Compliance (if handling credit cards)

```java
// ✓ Correct: Never store full credit card numbers
// Store only last 4 digits
@Data
public class PaymentMethod {
    private String cardLastFourDigits;  // 4567
    private String cardBrand;           // VISA
    private String expiryMonth;         // 12
    private String expiryYear;          // 2026
    
    // ✗ NEVER: Full card number
    // private String cardNumber;
}

// ✓ Correct: Use tokenization
public class PaymentGateway {
    
    public String tokenizeCard(CardDetails card) {
        // Send to PCI-compliant gateway
        // Receive back a token
        return paymentGateway.tokenize(card);
    }
}
```

#### Data Retention Policy

```java
// ✓ Correct: Implement data retention
@Component
@Scheduled(cron = "0 0 2 * * MON")  // Weekly
public class DataRetentionJob {
    
    public void cleanupOldData() {
        // Retain transactions forever (immutable ledger)
        
        // Retain audit logs for 7 years (banking requirement)
        LocalDateTime sevenYearsAgo = LocalDateTime.now().minusYears(7);
        
        // Retain refresh tokens for 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        refreshTokenRepository.deleteByCreatedAtBefore(thirtyDaysAgo);
        
        logger.info("Data cleanup completed");
    }
}
```

---

## 2. Performance Guidelines

### 2.1 Performance Targets

| Metric | Target | Max |
|--------|--------|-----|
| Response time (normal load) | < 200ms | 500ms |
| P99 response time | < 300ms | 1000ms |
| Throughput | 1000+ req/sec | - |
| Database connection pool | 10-20 | - |
| Memory usage | < 512MB | 1GB |

### 2.2 Database Optimization

#### Connection Pooling

```properties
# application.properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000
```

#### Query Optimization

```java
// ✓ Correct: Use indexes
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_account_date", columnList = "account_id, created_at"),
    @Index(name = "idx_reference_id", columnList = "reference_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class Transaction {
    // ...
}

// ✓ Correct: Projection (select only needed columns)
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    
    @Query("""
        SELECT new com.company.banking.dto.TransactionSummary(
            t.id, t.amount, t.type, t.createdAt
        )
        FROM Transaction t
        WHERE t.account.id = :accountId
        ORDER BY t.createdAt DESC
    """)
    Page<TransactionSummary> findSummaries(String accountId, Pageable pageable);
}

// ✗ Wrong: Fetches all columns
List<Transaction> findByAccountId(String accountId);

// ✓ Correct: Use pagination
@GetMapping("/accounts/{id}/transactions")
public Page<TransactionResponse> getTransactions(
    @PathVariable String id,
    @PageableDefault(size = 20, sort = "createdAt", direction = DESC) Pageable page) {
    return transactionService.getTransactions(id, page);
}

// ✗ Wrong: Fetches all transactions
@GetMapping("/accounts/{id}/transactions")
public List<TransactionResponse> getTransactions(@PathVariable String id) {
    return transactionService.getTransactions(id);  // 1M+ records!
}

// ✓ Correct: Use batch size for inserts
@Repository
public class TransactionRepository {
    
    public void insertBatch(List<Transaction> transactions) {
        for (int i = 0; i < transactions.size(); i++) {
            entityManager.persist(transactions.get(i));
            if ((i + 1) % 100 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }
}
```

#### N+1 Query Problem

```java
// ✗ Wrong: N+1 problem
@Entity
public class Account {
    @ManyToOne
    private Customer customer;  // Lazy by default
}

public List<AccountResponse> getAllAccounts() {
    return accountRepository.findAll().stream()
        .map(account -> {
            // Triggers query for each customer!
            return toResponse(account);  // account.getCustomer() → N+1
        })
        .collect(toList());
}

// ✓ Correct: Eager loading with join
@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    
    @Query("""
        SELECT DISTINCT a FROM Account a
        LEFT JOIN FETCH a.customer
        ORDER BY a.createdAt DESC
    """)
    Page<Account> findAllWithCustomer(Pageable pageable);
}

// OR use @EntityGraph
@EntityGraph(attributePaths = {"customer"})
Page<Account> findAll(Pageable pageable);
```

### 2.3 Caching Strategy

#### Redis Caching

```java
// ✓ Correct: Cache frequently accessed data
@Service
@Slf4j
public class AccountService {
    
    private final AccountRepository accountRepository;
    private final CacheManager cacheManager;
    
    @Cacheable(value = "accounts", key = "#id", unless = "#result == null")
    public Account getAccount(String id) {
        logger.debug("Cache miss for account: {}", id);
        return accountRepository.findById(id).orElse(null);
    }
    
    @CachePut(value = "accounts", key = "#account.id")
    public Account updateAccount(Account account) {
        return accountRepository.save(account);
    }
    
    @CacheEvict(value = "accounts", key = "#id")
    public void deleteAccount(String id) {
        accountRepository.deleteById(id);
    }
}

// ✓ Correct: Cache with TTL
@Bean
public RedisCacheConfiguration cacheConfiguration() {
    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10))  // 10 minutes
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer()
            )
        );
}

// ✓ Correct: Invalidate on balance change
@Service
public class TransactionService {
    
    @Transactional
    public Transaction withdraw(WithdrawCommand cmd) {
        Account account = accountRepository.findById(cmd.getAccountId());
        
        // Process withdrawal
        Transaction transaction = account.withdraw(cmd.getAmount());
        accountRepository.save(account);
        
        // Invalidate cache
        cacheManager.getCache("accounts").evict(cmd.getAccountId());
        
        return transaction;
    }
}
```

#### Cache Warming

```java
@Component
@Slf4j
public class CacheWarmer {
    
    @PostConstruct
    @Scheduled(fixedDelay = 3600000)  // Every hour
    public void warmCache() {
        logger.info("Starting cache warming...");
        
        // Pre-load roles
        List<Role> roles = roleRepository.findAll();
        roles.forEach(role -> cache.put("role:" + role.getId(), role));
        
        // Pre-load active customers count
        long activeCount = customerRepository.countByStatus(ACTIVE);
        cache.put("stats:active-customers", activeCount);
        
        logger.info("Cache warming completed");
    }
}
```

### 2.4 Async Processing

```java
// ✓ Correct: Offload non-critical operations
@Service
@Slf4j
public class TransactionService {
    
    private final TransactionRepository repository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    
    @Transactional
    public Transaction withdraw(WithdrawCommand cmd) {
        // Critical: update balance synchronously
        Account account = accountRepository.findById(cmd.getAccountId());
        Transaction transaction = account.withdraw(cmd.getAmount());
        repository.save(transaction);
        
        // Non-critical: async operations
        auditAsyncly(cmd);
        notifyAsyncly(cmd);
        
        return transaction;
    }
    
    @Async
    public void auditAsyncly(WithdrawCommand cmd) {
        try {
            auditService.log(cmd);
        } catch (Exception e) {
            logger.error("Audit failed", e);
        }
    }
    
    @Async
    public void notifyAsyncly(WithdrawCommand cmd) {
        try {
            notificationService.sendWithdrawalNotification(cmd);
        } catch (Exception e) {
            logger.error("Notification failed", e);
        }
    }
}

// Configuration
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
```

### 2.5 API Response Compression

```properties
# application.properties
server.compression.enabled=true
server.compression.min-response-size=1024
server.compression.mime-types=application/json,application/xml,text/html,text/xml,text/plain
```

### 2.6 Monitoring & Metrics

```java
// ✓ Correct: Track response times
@Component
@Aspect
@Slf4j
public class PerformanceMonitoring {
    
    @Around("@annotation(com.company.banking.common.Monitor)")
    public Object monitor(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Method: {} completed in {}ms", 
                joinPoint.getSignature().getName(), duration);
            
            // Alert if slow
            if (duration > 1000) {
                logger.warn("SLOW_METHOD: {} took {}ms", 
                    joinPoint.getSignature().getName(), duration);
            }
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Method: {} failed after {}ms", 
                joinPoint.getSignature().getName(), duration, e);
            throw e;
        }
    }
}
```

---

## 3. Security & Performance Checklist

### Pre-Production Checklist

- [ ] **Security**
  - [ ] All endpoints require authentication
  - [ ] RBAC properly configured
  - [ ] HTTPS/TLS enabled
  - [ ] JWT secret >= 32 chars
  - [ ] Password validation enforced
  - [ ] Sensitive data masked in logs
  - [ ] SQL injection prevented (parameterized queries)
  - [ ] CSRF protection enabled
  - [ ] Rate limiting configured
  - [ ] Audit logging enabled for critical operations

- [ ] **Performance**
  - [ ] Database indexes created
  - [ ] Connection pooling configured
  - [ ] N+1 queries eliminated
  - [ ] Pagination implemented
  - [ ] Redis caching configured
  - [ ] Slow queries identified and optimized
  - [ ] Response compression enabled
  - [ ] Load tests passed (1000 req/sec)
  - [ ] P99 response time < 1 second

- [ ] **Monitoring**
  - [ ] Health check endpoint working
  - [ ] Metrics exposed
  - [ ] Alerting configured
  - [ ] Logs aggregated (ELK/Datadog)
  - [ ] APM enabled (optional but recommended)

---
