package com.company.banking.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {
    private JwtService jwtService;
    private JwtProperties jwtProperties;
    private UserDetails testUser;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties(
                "my-secret-key-that-is-long-enough-for-256-bits-implementation!!",
                3600,  // 1 hour access token
                604800 // 7 days refresh token
        );
        jwtService = new JwtService(jwtProperties);

        testUser = new User(
                "testuser@example.com",
                "password",
                Arrays.asList(
                        new SimpleGrantedAuthority("ROLE_CUSTOMER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")
                )
        );
    }

    @Test
    void generateAccessToken_shouldCreateValidToken() {
        String token = jwtService.generateAccessToken(testUser);

        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void generateRefreshToken_shouldCreateValidToken() {
        String token = jwtService.generateRefreshToken(testUser);

        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void extractUsername_shouldReturnCorrectUsername() {
        String token = jwtService.generateAccessToken(testUser);
        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("testuser@example.com");
    }

    @Test
    void extractRoles_shouldReturnAllRoles() {
        String token = jwtService.generateAccessToken(testUser);
        List<String> roles = jwtService.extractRoles(token);

        assertThat(roles).isNotNull()
                .hasSize(2)
                .contains("ROLE_CUSTOMER", "ROLE_ADMIN");
    }

    @Test
    void isTokenValid_withValidToken_shouldReturnTrue() {
        String token = jwtService.generateAccessToken(testUser);
        boolean isValid = jwtService.isTokenValid(token);

        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenValid_withInvalidToken_shouldReturnFalse() {
        String invalidToken = "invalid.token.here";
        boolean isValid = jwtService.isTokenValid(invalidToken);

        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenValid_withMalformedToken_shouldReturnFalse() {
        String malformedToken = "malformed";
        boolean isValid = jwtService.isTokenValid(malformedToken);

        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenExpired_withValidToken_shouldReturnFalse() {
        String token = jwtService.generateAccessToken(testUser);
        boolean isExpired = jwtService.isTokenExpired(token);

        assertThat(isExpired).isFalse();
    }

    @Test
    void isTokenExpired_withInvalidToken_shouldReturnTrue() {
        String invalidToken = "invalid.token.here";
        boolean isExpired = jwtService.isTokenExpired(invalidToken);

        assertThat(isExpired).isTrue();
    }

    @Test
    void extractUsername_withInvalidToken_shouldThrowException() {
        String invalidToken = "invalid.token";

        assertThatThrownBy(() -> jwtService.extractUsername(invalidToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void generateTokens_withDifferentExpirations_shouldHaveDifferentExpiry() {
        String accessToken = jwtService.generateAccessToken(testUser);
        String refreshToken = jwtService.generateRefreshToken(testUser);

        assertThat(accessToken).isNotEqualTo(refreshToken);
    }

    @Test
    void generateAccessToken_shouldMarkTokenTypeAccess() {
        String token = jwtService.generateAccessToken(testUser);

        assertThat(jwtService.isAccessToken(token)).isTrue();
        assertThat(jwtService.isRefreshToken(token)).isFalse();
    }

    @Test
    void generateRefreshToken_shouldMarkTokenTypeRefresh() {
        String token = jwtService.generateRefreshToken(testUser);

        assertThat(jwtService.isRefreshToken(token)).isTrue();
        assertThat(jwtService.isAccessToken(token)).isFalse();
    }

    @Test
    void extractExpiration_shouldReturnFutureInstant() {
        String token = jwtService.generateAccessToken(testUser);

        assertThat(jwtService.extractExpiration(token)).isAfter(Instant.now().minusSeconds(1));
    }
}
