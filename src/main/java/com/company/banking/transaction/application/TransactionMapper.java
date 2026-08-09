package com.company.banking.transaction.application;

import com.company.banking.transaction.domain.Transaction;
import org.springframework.stereotype.Component;

/**
 * Maps domain {@link Transaction} to API response DTOs.
 */
@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.id(),
                transaction.accountId(),
                transaction.referenceId(),
                transaction.transactionType().name(),
                transaction.amount().amount(),
                transaction.balanceAfter().amount(),
                transaction.description(),
                transaction.createdAt()
        );
    }
}
