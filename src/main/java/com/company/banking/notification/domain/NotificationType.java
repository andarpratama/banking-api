package com.company.banking.notification.domain;

/**
 * High-level notification categories. Vendor adapters map these to channels/templates later.
 */
public enum NotificationType {
    TRANSFER_COMPLETED,
    DEPOSIT_COMPLETED,
    WITHDRAWAL_COMPLETED,
    AUTH_REGISTERED
}
