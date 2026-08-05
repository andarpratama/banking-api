package com.company.banking.auth.application;

import java.time.Instant;

public record RegisterResponse(
        String id,
        String email,
        String fullName,
        String customerId,
        Instant createdAt
) {
}
