package com.company.banking.transaction.infrastructure.persistence;

import com.company.banking.common.money.Money;
import com.company.banking.transaction.domain.Transaction;
import com.company.banking.transaction.domain.TransactionRepository;
import com.company.banking.transaction.domain.TransactionType;
import org.springframework.stereotype.Repository;

/**
 * Insert-only JPA adapter for the transaction ledger.
 */
@Repository
public class JpaTransactionRepository implements TransactionRepository {

    private final SpringDataTransactionRepository springDataRepository;

    public JpaTransactionRepository(SpringDataTransactionRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        if (springDataRepository.existsById(transaction.id())) {
            throw new IllegalStateException("Ledger rows are immutable; cannot update transaction "
                    + transaction.id());
        }

        TransactionJpaEntity entity = new TransactionJpaEntity(
                transaction.id(),
                transaction.accountId(),
                transaction.referenceId(),
                transaction.transactionType().name(),
                transaction.amount().amount(),
                transaction.balanceAfter().amount(),
                transaction.description(),
                transaction.createdAt()
        );

        TransactionJpaEntity saved = springDataRepository.save(entity);
        return toDomain(saved);
    }

    private Transaction toDomain(TransactionJpaEntity entity) {
        return new Transaction(
                entity.getId(),
                entity.getAccountId(),
                entity.getReferenceId(),
                TransactionType.valueOf(entity.getTransactionType()),
                Money.of(entity.getAmount()),
                Money.of(entity.getBalanceAfter()),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }
}
