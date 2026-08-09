package com.company.banking.transaction.infrastructure.audit;

import com.company.banking.transaction.application.DepositAuditPort;
import com.company.banking.transaction.domain.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * No-op audit hook until T-050 wires real audit persistence.
 */
@Component
public class NoOpDepositAuditPort implements DepositAuditPort {

    private static final Logger log = LoggerFactory.getLogger(NoOpDepositAuditPort.class);

    @Override
    public void onDeposit(Transaction transaction) {
        log.debug(
                "Deposit audit hook: transactionId={} accountId={} amount={}",
                transaction.id(),
                transaction.accountId(),
                transaction.amount()
        );
    }
}
