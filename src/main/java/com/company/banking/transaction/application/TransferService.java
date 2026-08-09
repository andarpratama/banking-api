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
 * Transfer use case: debit source + credit destination atomically with dual ledger rows.
 *
 * <p><b>Optimistic locking:</b> account rows use JPA {@code @Version}. Concurrent balance
 * updates that collide surface as {@code OPTIMISTIC_LOCK_EXCEPTION} (409). This service does
 * <em>not</em> auto-retry — the client should re-read balances and resubmit if needed.
 */
@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final SecurityContextHelper securityContextHelper;
    private final TransferAuditPort transferAuditPort;

    public TransferService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            TransactionMapper transactionMapper,
            SecurityContextHelper securityContextHelper,
            TransferAuditPort transferAuditPort
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.securityContextHelper = securityContextHelper;
        this.transferAuditPort = transferAuditPort;
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        if (request.getSourceAccountId().equals(request.getDestinationAccountId())) {
            throw new BusinessException(
                    ErrorCode.SAME_ACCOUNT_TRANSFER,
                    "Cannot transfer to same account"
            );
        }

        Money amount = Money.ofPositive(request.getAmount());

        Account source = accountRepository.findById(request.getSourceAccountId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ACCOUNT_NOT_FOUND,
                        "Source account not found with ID: " + request.getSourceAccountId()
                ));
        Account destination = accountRepository.findById(request.getDestinationAccountId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ACCOUNT_NOT_FOUND,
                        "Destination account not found with ID: " + request.getDestinationAccountId()
                ));

        if (!securityContextHelper.isAdmin()
                && !securityContextHelper.isOwner(source.customerId())) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Cannot transfer from another customer's account"
            );
        }

        Instant now = Instant.now();
        Account debited = source.debit(amount, now);
        Account credited = destination.credit(amount, now);

        // Persist lower UUID first to keep lock acquisition order stable across transfers.
        Account savedSource;
        Account savedDestination;
        if (source.id().compareTo(destination.id()) < 0) {
            savedSource = accountRepository.save(debited);
            savedDestination = accountRepository.save(credited);
        } else {
            savedDestination = accountRepository.save(credited);
            savedSource = accountRepository.save(debited);
        }

        UUID referenceId = UUID.randomUUID();
        Transaction debitLeg = Transaction.debit(
                UUID.randomUUID(),
                savedSource.id(),
                referenceId,
                amount,
                savedSource.balance(),
                request.getDescription(),
                now
        );
        Transaction creditLeg = Transaction.credit(
                UUID.randomUUID(),
                savedDestination.id(),
                referenceId,
                amount,
                savedDestination.balance(),
                request.getDescription(),
                now
        );

        Transaction persistedDebit = transactionRepository.save(debitLeg);
        Transaction persistedCredit = transactionRepository.save(creditLeg);
        transferAuditPort.onTransfer(referenceId, persistedDebit, persistedCredit);

        return new TransferResponse(
                referenceId,
                transactionMapper.toResponse(persistedDebit),
                transactionMapper.toResponse(persistedCredit)
        );
    }
}
