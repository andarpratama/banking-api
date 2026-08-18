package com.company.banking.transaction.application;

import com.company.banking.account.application.AccountCache;
import com.company.banking.account.domain.Account;
import com.company.banking.account.domain.AccountRepository;
import com.company.banking.common.constants.ErrorCode;
import com.company.banking.common.exception.BusinessException;
import com.company.banking.common.money.Money;
import com.company.banking.security.SecurityContextHelper;
import com.company.banking.transaction.domain.Transaction;
import com.company.banking.transaction.domain.TransactionRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deposit use case: validate account → credit balance → insert immutable ledger row.
 */
@Service
public class DepositService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final SecurityContextHelper securityContextHelper;
    private final DepositAuditPort depositAuditPort;
    private final AccountCache accountCache;

    public DepositService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            TransactionMapper transactionMapper,
            SecurityContextHelper securityContextHelper,
            DepositAuditPort depositAuditPort,
            AccountCache accountCache
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.securityContextHelper = securityContextHelper;
        this.depositAuditPort = depositAuditPort;
        this.accountCache = accountCache;
    }

    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        Money amount = Money.ofPositive(request.getAmount());

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ACCOUNT_NOT_FOUND,
                        "Account not found with ID: " + request.getAccountId()
                ));

        if (!securityContextHelper.isAdmin()
                && !securityContextHelper.isOwner(account.customerId())) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Cannot deposit to another customer's account"
            );
        }

        Instant now = Instant.now();
        Account credited = account.credit(amount, now);
        Account saved = accountRepository.save(credited);

        Transaction ledger = Transaction.deposit(
                UUID.randomUUID(),
                saved.id(),
                amount,
                saved.balance(),
                request.getDescription(),
                now
        );
        Transaction persisted = transactionRepository.save(ledger);
        depositAuditPort.onDeposit(persisted);
        accountCache.evict(saved.id());

        return transactionMapper.toResponse(persisted);
    }
}
