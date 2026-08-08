package com.company.banking.account.application;

import com.company.banking.account.domain.Account;
import com.company.banking.account.domain.AccountRepository;
import com.company.banking.account.domain.AccountUnfrozenEvent;
import com.company.banking.common.constants.ErrorCode;
import com.company.banking.common.exception.BusinessException;
import com.company.banking.common.money.Money;
import com.company.banking.customer.domain.CustomerRepository;
import com.company.banking.security.SecurityContextHelper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account application service — create, read, freeze, unfreeze, close.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AccountMapper accountMapper;
    private final SecurityContextHelper securityContextHelper;
    private final ApplicationEventPublisher eventPublisher;

    public AccountService(
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            AccountMapper accountMapper,
            SecurityContextHelper securityContextHelper,
            ApplicationEventPublisher eventPublisher
    ) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.accountMapper = accountMapper;
        this.securityContextHelper = securityContextHelper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (!securityContextHelper.isAdmin()
                && !securityContextHelper.isOwner(request.getCustomerId())) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Cannot create account for another customer"
            );
        }

        customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CUSTOMER_NOT_FOUND,
                        "Customer not found with ID: " + request.getCustomerId()
                ));

        BigDecimal initial = request.getInitialBalance() != null
                ? request.getInitialBalance()
                : BigDecimal.ZERO;
        Money balance = Money.ofNonNegative(initial);

        Instant now = Instant.now();
        String accountNumber = nextAccountNumber();
        Account account = Account.create(
                UUID.randomUUID(),
                request.getCustomerId(),
                accountNumber,
                request.getAccountType(),
                request.getCurrency(),
                balance,
                now
        );

        Account saved = accountRepository.save(account);
        return accountMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID accountId) {
        Account account = requireAccount(accountId);
        return accountMapper.toResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountListResponse listAccountsForCustomer(UUID customerId) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CUSTOMER_NOT_FOUND,
                        "Customer not found with ID: " + customerId
                ));

        List<AccountResponse> content = accountRepository.findByCustomerId(customerId).stream()
                .map(accountMapper::toResponse)
                .collect(Collectors.toList());
        long total = accountRepository.countByCustomerId(customerId);
        return new AccountListResponse(content, total);
    }

    @Transactional
    public AccountStatusResponse freezeAccount(UUID accountId) {
        Account account = requireAccount(accountId);
        Account frozen = account.freeze(Instant.now());
        Account saved = accountRepository.save(frozen);
        return accountMapper.toStatusResponse(saved);
    }

    @Transactional
    public AccountStatusResponse unfreezeAccount(UUID accountId) {
        Account account = requireAccount(accountId);
        Instant now = Instant.now();
        Account unfrozen = account.unfreeze(now);
        Account saved = accountRepository.save(unfrozen);
        eventPublisher.publishEvent(
                new AccountUnfrozenEvent(saved.id(), saved.accountNumber(), saved.updatedAt())
        );
        return accountMapper.toStatusResponse(saved);
    }

    @Transactional
    public AccountStatusResponse closeAccount(UUID accountId) {
        Account account = requireAccount(accountId);
        Account closed = account.close(Instant.now());
        Account saved = accountRepository.save(closed);
        return accountMapper.toStatusResponse(saved);
    }

    private Account requireAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ACCOUNT_NOT_FOUND,
                        "Account not found with ID: " + accountId
                ));
    }

    private String nextAccountNumber() {
        int next = accountRepository.nextAccountSequence();
        return String.format("ACC-%07d", next);
    }
}
