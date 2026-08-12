package com.company.banking.notification.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable outbound notification payload. Framework-free so money/auth flows can publish
 * without coupling to email/SMS/push vendors.
 */
public final class NotificationMessage {

    private final UUID recipientId;
    private final NotificationType type;
    private final String title;
    private final String body;
    private final Instant occurredAt;

    public NotificationMessage(
            UUID recipientId,
            NotificationType type,
            String title,
            String body,
            Instant occurredAt
    ) {
        this.recipientId = Objects.requireNonNull(recipientId, "recipientId");
        this.type = Objects.requireNonNull(type, "type");
        this.title = requireText(title, "title");
        this.body = requireText(body, "body");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
    }

    public static NotificationMessage transferCompleted(
            UUID recipientCustomerId,
            UUID referenceId,
            String amount,
            Instant occurredAt
    ) {
        Objects.requireNonNull(referenceId, "referenceId");
        Objects.requireNonNull(amount, "amount");
        return new NotificationMessage(
                recipientCustomerId,
                NotificationType.TRANSFER_COMPLETED,
                "Transfer completed",
                "Transfer " + referenceId + " for amount " + amount + " completed",
                occurredAt
        );
    }

    public UUID recipientId() {
        return recipientId;
    }

    public NotificationType type() {
        return type;
    }

    public String title() {
        return title;
    }

    public String body() {
        return body;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return trimmed;
    }
}
