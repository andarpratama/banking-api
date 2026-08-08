package com.company.banking.account.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event raised after a successful FROZEN → ACTIVE transition.
 * Audit (T-050) can listen without coupling account to the audit feature.
 */
public record AccountUnfrozenEvent(UUID accountId, String accountNumber, Instant occurredAt) {
}
