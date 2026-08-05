package com.company.banking.auth.application;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserInfo user
) {
    public record UserInfo(
            String id,
            String email,
            List<String> roles
    ) {
    }
}
