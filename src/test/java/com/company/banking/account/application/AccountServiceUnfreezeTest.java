package com.company.banking.account.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.banking.account.domain.Account;
import com.company.banking.account.domain.AccountRepository;
import com.company.banking.account.domain.AccountType;
import com.company.banking.account.domain.AccountUnfrozenEvent;
import com.company.banking.common.money.Money;
import com.company.banking.customer.domain.CustomerRepository;
import com.company.banking.security.SecurityContextHelper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Application-layer unfreeze tests (status update + audit event hook).
 */
class AccountServiceUnfreezeTest {

    private AccountRepository accountRepository;
    private ApplicationEventPublisher eventPublisher;
    private AccountService accountService;

    private UUID accountId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        accountService = new AccountService(
                accountRepository,
                mock(CustomerRepository.class),
                new AccountMapper(),
                mock(SecurityContextHelper.class),
                eventPublisher
        );
        accountId = UUID.randomUUID();
        customerId = UUID.randomUUID();
    }

    @Test
    void unfreezeAccountPersistsActiveAndPublishesAuditHookEvent() {
        Instant now = Instant.parse("2026-08-09T12:00:00Z");
        Account frozen = Account.create(
                accountId,
                customerId,
                "ACC-0000042",
                AccountType.CHECKING,
                "USD",
                Money.zero(),
                now
        ).freeze(now);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(frozen));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountStatusResponse response = accountService.unfreezeAccount(accountId);

        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getId()).isEqualTo(accountId);

        ArgumentCaptor<AccountUnfrozenEvent> captor = ArgumentCaptor.forClass(AccountUnfrozenEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().accountId()).isEqualTo(accountId);
        assertThat(captor.getValue().accountNumber()).isEqualTo("ACC-0000042");
    }
}
