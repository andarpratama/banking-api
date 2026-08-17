package com.company.banking.auth.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Login credentials")
public record LoginRequest(
        @Schema(example = "customer@example.com")
        @NotBlank @Email @Size(max = 255) String email,
        @Schema(example = "SecurePass123!")
        @NotBlank @Size(max = 128) String password
) {
}
