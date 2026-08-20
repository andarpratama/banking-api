package com.company.banking.auth.presentation;

import com.company.banking.auth.application.AuthService;
import com.company.banking.auth.application.LoginResponse;
import com.company.banking.auth.application.RegisterResponse;
import com.company.banking.auth.application.TokenResponse;
import com.company.banking.common.presentation.OpenApiExamples;
import com.company.banking.common.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register, login, refresh, logout")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirements
    @Operation(summary = "Register customer", description = "Creates a user + customer profile with CUSTOMER role.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = RegisterRequest.class),
                    examples = @ExampleObject(name = "New customer", value = OpenApiExamples.REGISTER_REQUEST)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Registered",
                    content = @Content(
                            schema = @Schema(implementation = RegisterResponse.class),
                            examples = @ExampleObject(
                                    name = "Registered customer",
                                    value = OpenApiExamples.REGISTER_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error or duplicate email",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Duplicate email",
                                    value = OpenApiExamples.ERROR_DUPLICATE_EMAIL
                            )
                    )
            )
    })
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse body = authService.register(
                request.email(),
                request.password(),
                request.fullName(),
                request.phone(),
                request.address()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirements
    @Operation(summary = "Login", description = "Returns access + refresh tokens. Password is never returned.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = LoginRequest.class),
                    examples = @ExampleObject(name = "Customer login", value = OpenApiExamples.LOGIN_REQUEST)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Authenticated",
                    content = @Content(
                            schema = @Schema(implementation = LoginResponse.class),
                            examples = @ExampleObject(
                                    name = "Authenticated session",
                                    value = OpenApiExamples.LOGIN_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Invalid credentials",
                                    value = OpenApiExamples.ERROR_INVALID_CREDENTIALS
                            )
                    )
            )
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirements
    @Operation(summary = "Refresh tokens", description = "Rotates refresh token and issues a new access token.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = RefreshRequest.class),
                    examples = @ExampleObject(name = "Refresh token", value = OpenApiExamples.REFRESH_REQUEST)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tokens rotated",
                    content = @Content(
                            schema = @Schema(implementation = TokenResponse.class),
                            examples = @ExampleObject(
                                    name = "Rotated tokens",
                                    value = OpenApiExamples.TOKEN_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired refresh token",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Invalid refresh token",
                                    value = OpenApiExamples.ERROR_INVALID_TOKEN
                            )
                    )
            )
    })
    public TokenResponse refresh(
            @Valid @RequestBody RefreshRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        String token = resolveRefreshToken(request.refreshToken(), authorization);
        return authService.refresh(token);
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Logout", description = "Blacklists the access token and revokes refresh tokens for the user.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logged out"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid access token",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Missing access token",
                                    value = OpenApiExamples.ERROR_UNAUTHORIZED
                            )
                    )
            )
    })
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        authService.logout(extractBearer(authorization));
        return ResponseEntity.noContent().build();
    }

    private static String resolveRefreshToken(String bodyToken, String authorization) {
        if (bodyToken != null && !bodyToken.isBlank()) {
            return bodyToken.trim();
        }
        return extractBearer(authorization);
    }

    private static String extractBearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7).trim();
    }
}
