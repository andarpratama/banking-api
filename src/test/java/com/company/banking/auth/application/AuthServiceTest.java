package com.company.banking.auth.application;

import com.company.banking.auth.domain.CustomerProfile;
import com.company.banking.auth.domain.CustomerProfileRepository;
import com.company.banking.auth.domain.RefreshTokenRecord;
import com.company.banking.auth.domain.RefreshTokenRepository;
import com.company.banking.auth.domain.TokenHasher;
import com.company.banking.auth.domain.UserAccount;
import com.company.banking.auth.domain.UserAccountRepository;
import com.company.banking.common.constants.ErrorCode;
import com.company.banking.common.exception.BusinessException;
import com.company.banking.security.AccessTokenBlacklist;
import com.company.banking.security.JwtProperties;
import com.company.banking.security.JwtService;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository users;
    @Mock
    private CustomerProfileRepository customers;
    @Mock
    private RefreshTokenRepository refreshTokens;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AccessTokenBlacklist accessTokenBlacklist;

    private JwtProperties jwtProperties;
    private AuthService authService;

    private final UUID userId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties("secret-key-that-is-long-enough-for-tests-256bits!!", 3600, 604800);
        authService = new AuthService(
                users,
                customers,
                refreshTokens,
                passwordEncoder,
                jwtService,
                jwtProperties,
                accessTokenBlacklist
        );
    }

    @Test
    void register_shouldCreateUserAndCustomerWithoutReturningPassword() {
        when(users.existsByEmail("customer@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("hashed");
        UserAccount savedUser = user("customer@example.com", "hashed", Set.of("CUSTOMER"));
        when(users.save("customer@example.com", "hashed", "CUSTOMER")).thenReturn(savedUser);
        when(customers.create(eq(userId), eq("John Doe"), eq("+1-555"), eq("Street")))
                .thenReturn(new CustomerProfile(
                        customerId, userId, "CUST-000002", "John Doe", "+1-555", "Street", Instant.parse("2026-08-04T12:00:00Z")
                ));

        RegisterResponse response = authService.register(
                "Customer@Example.com",
                "SecurePass123!",
                "John Doe",
                "+1-555",
                "Street"
        );

        assertThat(response.id()).isEqualTo(userId.toString());
        assertThat(response.email()).isEqualTo("customer@example.com");
        assertThat(response.fullName()).isEqualTo("John Doe");
        assertThat(response.customerId()).isEqualTo(customerId.toString());
        assertThat(response.toString()).doesNotContain("SecurePass123!");
        assertThat(response.toString()).doesNotContain("hashed");
    }

    @Test
    void register_duplicateEmail_shouldThrow() {
        when(users.existsByEmail("customer@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                "customer@example.com", "SecurePass123!", "John", null, null
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    void login_validCredentials_shouldIssueTokensAndPersistRefreshHash() {
        UserAccount account = user("customer@example.com", "hashed", Set.of("CUSTOMER"));
        when(users.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("SecurePass123!", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(any(UserDetails.class))).thenReturn("access.jwt");
        when(jwtService.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh.jwt");

        LoginResponse response = authService.login("customer@example.com", "SecurePass123!");

        assertThat(response.accessToken()).isEqualTo("access.jwt");
        assertThat(response.refreshToken()).isEqualTo("refresh.jwt");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600);
        assertThat(response.user().roles()).containsExactly("CUSTOMER");
        assertThat(response.toString()).doesNotContain("hashed");

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(refreshTokens).save(eq(userId), hashCaptor.capture(), any(Instant.class));
        assertThat(hashCaptor.getValue()).isEqualTo(TokenHasher.sha256("refresh.jwt"));
    }

    @Test
    void login_invalidPassword_shouldThrowInvalidCredentials() {
        UserAccount account = user("customer@example.com", "hashed", Set.of("CUSTOMER"));
        when(users.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("customer@example.com", "wrong"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void refresh_shouldRotateToken() {
        String oldRefresh = "old.refresh.jwt";
        String hash = TokenHasher.sha256(oldRefresh);
        UUID tokenId = UUID.randomUUID();
        RefreshTokenRecord stored = new RefreshTokenRecord(
                tokenId, userId, hash, Instant.now().plusSeconds(3600), false, Instant.now()
        );
        UserAccount account = user("customer@example.com", "hashed", Set.of("CUSTOMER"));

        when(jwtService.isTokenValid(oldRefresh)).thenReturn(true);
        when(jwtService.isRefreshToken(oldRefresh)).thenReturn(true);
        when(refreshTokens.findByTokenHash(hash)).thenReturn(Optional.of(stored));
        when(users.findById(userId)).thenReturn(Optional.of(account));
        when(jwtService.generateAccessToken(any(UserDetails.class))).thenReturn("new.access");
        when(jwtService.generateRefreshToken(any(UserDetails.class))).thenReturn("new.refresh");

        TokenResponse response = authService.refresh(oldRefresh);

        assertThat(response.accessToken()).isEqualTo("new.access");
        assertThat(response.refreshToken()).isEqualTo("new.refresh");
        verify(refreshTokens).revoke(tokenId);
        verify(refreshTokens).save(eq(userId), eq(TokenHasher.sha256("new.refresh")), any(Instant.class));
    }

    @Test
    void refresh_revokedToken_shouldThrowInvalidToken() {
        String oldRefresh = "old.refresh.jwt";
        String hash = TokenHasher.sha256(oldRefresh);
        RefreshTokenRecord stored = new RefreshTokenRecord(
                UUID.randomUUID(), userId, hash, Instant.now().plusSeconds(3600), true, Instant.now()
        );

        when(jwtService.isTokenValid(oldRefresh)).thenReturn(true);
        when(jwtService.isRefreshToken(oldRefresh)).thenReturn(true);
        when(refreshTokens.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(oldRefresh))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void logout_shouldBlacklistAccessTokenAndRevokeRefreshTokens() {
        String access = "access.jwt";
        Instant expiresAt = Instant.now().plusSeconds(600);
        UserAccount account = user("customer@example.com", "hashed", Set.of("CUSTOMER"));

        when(jwtService.isTokenValid(access)).thenReturn(true);
        when(jwtService.isAccessToken(access)).thenReturn(true);
        when(accessTokenBlacklist.isBlacklisted(access)).thenReturn(false);
        when(jwtService.extractUsername(access)).thenReturn("customer@example.com");
        when(users.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(jwtService.extractExpiration(access)).thenReturn(expiresAt);

        authService.logout(access);

        verify(accessTokenBlacklist).blacklist(access, expiresAt);
        verify(refreshTokens).revokeAllForUser(userId);
    }

    @Test
    void logout_missingToken_shouldThrowUnauthorized() {
        assertThatThrownBy(() -> authService.logout(null))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    private UserAccount user(String email, String hash, Set<String> roles) {
        Instant now = Instant.parse("2026-08-04T12:00:00Z");
        return new UserAccount(userId, email, hash, true, now, now, roles);
    }
}
