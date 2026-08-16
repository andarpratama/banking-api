package com.company.banking.auth.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Customer registration payload")
public record RegisterRequest(
        @Schema(example = "customer@example.com")
        @NotBlank @Email @Size(max = 255) String email,
        @Schema(example = "SecurePass123!")
        @NotBlank @Size(min = 8, max = 128) String password,
        @Schema(example = "John Doe")
        @NotBlank @Size(max = 255) String fullName,
        @Schema(example = "+1-555-0123")
        @Size(max = 20) String phone,
        @Schema(example = "123 Main St, City, State 12345")
        @Size(max = 500) String address
) {
}
