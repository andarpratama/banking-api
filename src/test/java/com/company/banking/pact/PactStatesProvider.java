package com.company.banking.pact;

import com.company.banking.account.domain.Account;
import com.company.banking.account.domain.AccountRepository;
import com.company.banking.account.domain.AccountType;
import com.company.banking.auth.application.AuthService;
import com.company.banking.auth.application.LoginResponse;
import com.company.banking.auth.application.RegisterResponse;
import com.company.banking.auth.domain.UserAccount;
import com.company.banking.auth.domain.UserAccountRepository;
import com.company.banking.common.money.Money;
import com.company.banking.customer.domain.Customer;
import com.company.banking.customer.domain.CustomerRepository;
import com.company.banking.transaction.domain.Transaction;
import com.company.banking.transaction.domain.TransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Seeds provider state for Pact verification. Each state method commits before
 * the HTTP interaction runs (no test-level transaction wrapping the web thread).
 */
@Component
public class PactStatesProvider {

    private final AuthService authService;
    private final UserAccountRepository users;
    private final CustomerRepository customers;
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final JdbcTemplate jdbcTemplate;
    private final PactVerificationFixture fixture = new PactVerificationFixture();

    public PactStatesProvider(
            AuthService authService,
            UserAccountRepository users,
            CustomerRepository customers,
            AccountRepository accounts,
            TransactionRepository transactions,
            JdbcTemplate jdbcTemplate
    ) {
        this.authService = authService;
        this.users = users;
        this.customers = customers;
        this.accounts = accounts;
        this.transactions = transactions;
        this.jdbcTemplate = jdbcTemplate;
    }

    PactVerificationFixture fixture() {
        return fixture;
    }

    public void registerEmailIsAvailable() {
        deleteUserByEmail(PactContractSupport.REGISTER_EMAIL);
    }

    public void customerExistsForLogin() {
        ensureCustomer(PactContractSupport.CUSTOMER_EMAIL, PactContractSupport.CUSTOMER_NAME);
    }

    public void customerIsAuthenticated() {
        loginCustomer(PactContractSupport.CUSTOMER_EMAIL, PactContractSupport.CUSTOMER_NAME);
    }

    public void customerIsLoggedIn() {
        customerIsAuthenticated();
    }

    public void customerHasValidRefreshToken() {
        customerIsAuthenticated();
        // JWT iat is second-precision; refresh in the same second reuses the hash and 500s.
        sleepOneSecond();
    }

    public void customerHasActiveAccount() {
        customerIsAuthenticated();
        Account account = newAccount(fixture.customerId(), "1000.00");
        fixture.setAccountId(account.id().toString());
    }

    public void customerHasTwoActiveAccounts() {
        customerIsAuthenticated();
        Account source = newAccount(fixture.customerId(), "1000.00");
        Account destination = newAccount(fixture.customerId(), "100.00");
        fixture.setAccountId(source.id().toString());
        fixture.setDestinationAccountId(destination.id().toString());
    }

    public void accountHasDepositHistory() {
        customerHasActiveAccount();
        Account account = accounts.findById(UUID.fromString(fixture.accountId())).orElseThrow();
        Instant now = Instant.now();
        Money amount = Money.ofPositive(new BigDecimal("100.00"));
        Account credited = account.credit(amount, now);
        accounts.save(credited);
        transactions.save(Transaction.deposit(
                UUID.randomUUID(),
                credited.id(),
                amount,
                credited.balance(),
                "Pact deposit",
                now
        ));
    }

    public void adminIsAuthenticated() {
        loginAdmin();
    }

    public void adminCanDeleteCustomer() {
        loginAdmin();
        loginCustomer(PactContractSupport.DELETE_EMAIL, "Pact Delete");
    }

    public void adminCanFreezeCustomerAccount() {
        customerHasActiveAccount();
        loginAdmin();
    }

    public void adminCanUnfreezeFrozenAccount() {
        customerHasActiveAccount();
        Account account = accounts.findById(UUID.fromString(fixture.accountId())).orElseThrow();
        accounts.save(account.freeze(Instant.now()));
        loginAdmin();
    }

    private void loginCustomer(String email, String fullName) {
        ensureCustomer(email, fullName);
        UserAccount user = users.findByEmail(email).orElseThrow();
        Customer customer = customers.findByUserId(user.id()).orElseThrow();
        if (!fullName.equals(customer.fullName())
                || !PactContractSupport.PHONE.equals(customer.phone())
                || !PactContractSupport.ADDRESS.equals(customer.address())) {
            customer = customers.save(customer.updateProfile(
                    fullName,
                    PactContractSupport.PHONE,
                    PactContractSupport.ADDRESS,
                    Instant.now()
            ));
        }
        revokeRefreshTokens(email);
        LoginResponse login = authService.login(email, PactContractSupport.PASSWORD);
        fixture.setCustomerId(customer.id().toString());
        fixture.setCustomerAccessToken(login.accessToken());
        fixture.setRefreshToken(login.refreshToken());
    }

    private void loginAdmin() {
        ensureCustomer(PactContractSupport.ADMIN_EMAIL, PactContractSupport.ADMIN_NAME);
        grantAdminRole(PactContractSupport.ADMIN_EMAIL);
        revokeRefreshTokens(PactContractSupport.ADMIN_EMAIL);
        LoginResponse login = authService.login(
                PactContractSupport.ADMIN_EMAIL,
                PactContractSupport.PASSWORD
        );
        fixture.setAdminAccessToken(login.accessToken());
    }

    private void ensureCustomer(String email, String fullName) {
        if (!users.existsByEmail(email)) {
            RegisterResponse registered = authService.register(
                    email,
                    PactContractSupport.PASSWORD,
                    fullName,
                    PactContractSupport.PHONE,
                    PactContractSupport.ADDRESS
            );
            fixture.setCustomerId(registered.customerId());
        }
    }

    private Account newAccount(String customerId, String initialBalance) {
        Instant now = Instant.now();
        String accountNumber = String.format("ACC-%07d", accounts.nextAccountSequence());
        Account account = Account.create(
                UUID.randomUUID(),
                UUID.fromString(customerId),
                accountNumber,
                AccountType.SAVINGS,
                "USD",
                Money.ofNonNegative(new BigDecimal(initialBalance)),
                now
        );
        return accounts.save(account);
    }

    private void sleepOneSecond() {
        try {
            Thread.sleep(1100);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for distinct JWT iat", ex);
        }
    }

    private void revokeRefreshTokens(String email) {
        jdbcTemplate.update(
                """
                        DELETE FROM refresh_tokens
                        WHERE user_id IN (
                            SELECT id FROM users WHERE lower(email) = lower(?)
                        )
                        """,
                email
        );
    }

    private void grantAdminRole(String email) {
        jdbcTemplate.update(
                """
                        INSERT INTO user_roles (user_id, role_id)
                        SELECT u.id, r.id
                        FROM users u
                        CROSS JOIN roles r
                        WHERE lower(u.email) = lower(?)
                          AND r.name = 'ADMIN'
                        ON CONFLICT DO NOTHING
                        """,
                email
        );
    }

    private void deleteUserByEmail(String email) {
        jdbcTemplate.update(
                """
                        DELETE FROM transactions
                        WHERE account_id IN (
                            SELECT a.id FROM accounts a
                            INNER JOIN customers c ON c.id = a.customer_id
                            INNER JOIN users u ON u.id = c.user_id
                            WHERE lower(u.email) = lower(?)
                        )
                        """,
                email
        );
        jdbcTemplate.update(
                """
                        DELETE FROM accounts
                        WHERE customer_id IN (
                            SELECT c.id FROM customers c
                            INNER JOIN users u ON u.id = c.user_id
                            WHERE lower(u.email) = lower(?)
                        )
                        """,
                email
        );
        jdbcTemplate.update(
                """
                        DELETE FROM refresh_tokens
                        WHERE user_id IN (
                            SELECT id FROM users WHERE lower(email) = lower(?)
                        )
                        """,
                email
        );
        jdbcTemplate.update("DELETE FROM users WHERE lower(email) = lower(?)", email);
    }
}
