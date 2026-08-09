package com.company.banking.transaction.infrastructure.audit;

import com.company.banking.transaction.application.WithdrawAuditPort;
import com.company.banking.transaction.domain.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * No-op audit hook until T-050 wires real audit persistence.
 */
@Component
public class NoOpWithdrawAuditPort implements WithdrawAuditPort {

    private static final Logger log = LoggerFactory.getLogger(NoOpWithdrawAuditPort.class);

    @Override
    public void onWithdraw(Transaction transaction) {
        log.debug(
                "Withdraw audit hook: transactionId={} accountId={} amount={}",
                transaction.id(),
                transaction.accountId(),
                transaction.amount()
        );
    }
}
