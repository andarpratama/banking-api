package com.company.banking.transaction.infrastructure.audit;

import com.company.banking.transaction.application.TransferAuditPort;
import com.company.banking.transaction.domain.Transaction;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * No-op audit hook until T-050 wires real audit persistence.
 */
@Component
public class NoOpTransferAuditPort implements TransferAuditPort {

    private static final Logger log = LoggerFactory.getLogger(NoOpTransferAuditPort.class);

    @Override
    public void onTransfer(UUID referenceId, Transaction debit, Transaction credit) {
        log.debug(
                "Transfer audit hook: referenceId={} debitId={} creditId={} amount={}",
                referenceId,
                debit.id(),
                credit.id(),
                debit.amount()
        );
    }
}
