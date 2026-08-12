package com.company.banking.transaction.application;

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
 * Withdraw use case: validate account → debit balance → insert immutable ledger row.
 */
@Service
public class WithdrawService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final SecurityContextHelper securityContextHelper;
    private final WithdrawAuditPort withdrawAuditPort;

    public WithdrawService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            TransactionMapper transactionMapper,
            SecurityContextHelper securityContextHelper,
            WithdrawAuditPort withdrawAuditPort
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.securityContextHelper = securityContextHelper;
        this.withdrawAuditPort = withdrawAuditPort;
    }

    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request) {
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
                    "Cannot withdraw from another customer's account"
            );
        }

        Instant now = Instant.now();
        Account debited = account.debit(amount, now);
        Account saved = accountRepository.save(debited);

        Transaction ledger = Transaction.withdraw(
                UUID.randomUUID(),
                saved.id(),
                amount,
                saved.balance(),
                request.getDescription(),
                now
        );
        Transaction persisted = transactionRepository.save(ledger);
        withdrawAuditPort.onWithdraw(persisted);

        return transactionMapper.toResponse(persisted);
    }
}
