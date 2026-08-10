package com.company.banking.audit.infrastructure;

import com.company.banking.audit.application.AuditPayloadHasher;
import com.company.banking.audit.application.AuditService;
import com.company.banking.audit.application.RecordAuditCommand;
import com.company.banking.audit.domain.AuditActions;
import com.company.banking.security.SecurityContextHelper;
import com.company.banking.transaction.application.WithdrawAuditPort;
import com.company.banking.transaction.domain.Transaction;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Persists withdraw audit entries.
 */
@Component
@Primary
public class WithdrawAuditAdapter implements WithdrawAuditPort {

    private final AuditService auditService;
    private final SecurityContextHelper securityContextHelper;

    public WithdrawAuditAdapter(AuditService auditService, SecurityContextHelper securityContextHelper) {
        this.auditService = auditService;
        this.securityContextHelper = securityContextHelper;
    }

    @Override
    public void onWithdraw(Transaction transaction) {
        String actor = resolveActor();
        auditService.record(RecordAuditCommand.of(
                actor,
                "/transactions/withdraw",
                "POST",
                AuditActions.WITHDRAW,
                200,
                null,
                AuditPayloadHasher.sha256("transactionId=" + transaction.id()
                        + ";accountId=" + transaction.accountId()
                        + ";amount=" + transaction.amount().amount())
        ));
    }

    private String resolveActor() {
        String username = securityContextHelper.getCurrentUsername();
        return username != null ? username : "system";
    }
}
