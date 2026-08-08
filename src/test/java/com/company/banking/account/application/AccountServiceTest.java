package com.company.banking.account.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.banking.account.domain.Account;
import com.company.banking.account.domain.AccountRepository;
import com.company.banking.account.domain.AccountStatus;
import com.company.banking.account.domain.AccountType;
import com.company.banking.account.domain.AccountUnfrozenEvent;
import com.company.banking.common.constants.ErrorCode;
import com.company.banking.common.exception.BusinessException;
import com.company.banking.common.money.Money;
import com.company.banking.customer.domain.Customer;
import com.company.banking.customer.domain.CustomerRepository;
import com.company.banking.customer.domain.CustomerStatus;
import com.company.banking.security.SecurityContextHelper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class AccountServiceTest {

    private AccountRepository accountRepository;
    private CustomerRepository customerRepository;
    private AccountMapper accountMapper;
    private SecurityContextHelper securityContextHelper;
    private ApplicationEventPublisher eventPublisher;
    private AccountService accountService;

    private UUID customerId;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        customerRepository = mock(CustomerRepository.class);
        accountMapper = new AccountMapper();
        securityContextHelper = mock(SecurityContextHelper.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        accountService = new AccountService(
                accountRepository,
                customerRepository,
                accountMapper,
                securityContextHelper,
                eventPublisher
        );
        customerId = UUID.randomUUID();
        accountId = UUID.randomUUID();
    }

    @Test
    void createAccountPersistsWithGeneratedNumber() {
        when(securityContextHelper.isAdmin()).thenReturn(true);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer()));
        when(accountRepository.nextAccountSequence()).thenReturn(1);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateAccountRequest request = new CreateAccountRequest(
                customerId,
                AccountType.SAVINGS,
                "USD",
                new BigDecimal("50.00")
        );

        AccountResponse response = accountService.createAccount(request);

        assertThat(response.getAccountNumber()).isEqualTo("ACC-0000001");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getBalance()).isEqualByComparingTo("50.00");
        assertThat(response.getVersion()).isZero();
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createAccountForbiddenForNonOwnerCustomer() {
        when(securityContextHelper.isAdmin()).thenReturn(false);
        when(securityContextHelper.isOwner(customerId)).thenReturn(false);

        CreateAccountRequest request = new CreateAccountRequest(
                customerId,
                AccountType.SAVINGS,
                "USD",
                BigDecimal.ZERO
        );

        assertThatThrownBy(() -> accountService.createAccount(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void freezeAccountUpdatesStatus() {
        Account active = sampleAccount(AccountStatus.ACTIVE);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(active));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountStatusResponse response = accountService.freezeAccount(accountId);

        assertThat(response.getStatus()).isEqualTo("FROZEN");
        assertThat(response.getAccountNumber()).isEqualTo(active.accountNumber());
    }

    @Test
    void unfreezeAccountUpdatesStatusAndPublishesEvent() {
        Account frozen = sampleAccount(AccountStatus.FROZEN);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(frozen));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountStatusResponse response = accountService.unfreezeAccount(accountId);

        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getAccountNumber()).isEqualTo(frozen.accountNumber());

        ArgumentCaptor<AccountUnfrozenEvent> eventCaptor = ArgumentCaptor.forClass(AccountUnfrozenEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        AccountUnfrozenEvent event = eventCaptor.getValue();
        assertThat(event.accountId()).isEqualTo(accountId);
        assertThat(event.accountNumber()).isEqualTo(frozen.accountNumber());
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void getAccountThrowsWhenMissing() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(accountId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    @Test
    void listAccountsForCustomerReturnsContent() {
        Account account = sampleAccount(AccountStatus.ACTIVE);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer()));
        when(accountRepository.findByCustomerId(customerId)).thenReturn(List.of(account));
        when(accountRepository.countByCustomerId(customerId)).thenReturn(1L);

        AccountListResponse response = accountService.listAccountsForCustomer(customerId);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getId()).isEqualTo(account.id());
    }

    private Customer sampleCustomer() {
        Instant now = Instant.now();
        return new Customer(
                customerId,
                UUID.randomUUID(),
                "CUST-000001",
                "Jane Doe",
                null,
                null,
                CustomerStatus.ACTIVE,
                now,
                now
        );
    }

    private Account sampleAccount(AccountStatus status) {
        Instant now = Instant.now();
        Account account = Account.create(
                accountId,
                customerId,
                "ACC-0000001",
                AccountType.SAVINGS,
                "USD",
                Money.zero(),
                now
        );
        if (status == AccountStatus.FROZEN) {
            return account.freeze(now);
        }
        if (status == AccountStatus.CLOSED) {
            return account.close(now);
        }
        return account;
    }
}
