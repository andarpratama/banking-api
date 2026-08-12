package com.company.banking.audit.infrastructure;

import com.company.banking.audit.application.AuditPayloadHasher;
import com.company.banking.audit.application.AuditService;
import com.company.banking.audit.application.RecordAuditCommand;
import com.company.banking.audit.domain.AuditActions;
import com.company.banking.security.SecurityContextHelper;
import com.company.banking.transaction.application.TransferAuditPort;
import com.company.banking.transaction.domain.Transaction;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Persists transfer audit entries.
 */
@Component
@Primary
public class TransferAuditAdapter implements TransferAuditPort {

    private final AuditService auditService;
    private final SecurityContextHelper securityContextHelper;

    public TransferAuditAdapter(AuditService auditService, SecurityContextHelper securityContextHelper) {
        this.auditService = auditService;
        this.securityContextHelper = securityContextHelper;
    }

    @Override
    public void onTransfer(UUID referenceId, Transaction debit, Transaction credit) {
        String actor = resolveActor();
        auditService.record(RecordAuditCommand.of(
                actor,
                "/transactions/transfer",
                "POST",
                AuditActions.TRANSFER_MONEY,
                200,
                null,
                AuditPayloadHasher.sha256("referenceId=" + referenceId
                        + ";fromAccountId=" + debit.accountId()
                        + ";toAccountId=" + credit.accountId()
                        + ";amount=" + debit.amount().amount())
        ));
    }

    private String resolveActor() {
        String username = securityContextHelper.getCurrentUsername();
        return username != null ? username : "system";
    }
}
