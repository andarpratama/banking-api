package com.company.banking.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private AccessTokenBlacklist accessTokenBlacklist;

    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private String testToken;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, accessTokenBlacklist);

        secretKey = Keys.hmacShaKeyFor(
                "test-secret-key-that-is-long-enough-for-256-bits-implementation!!"
                        .getBytes(StandardCharsets.UTF_8)
        );
        testToken = generateValidToken();
    }

    private String generateValidToken() {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("testuser@example.com")
                .claim("roles", Arrays.asList("ROLE_CUSTOMER"))
                .claim("enabled", true)
                .claim("typ", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void doFilterInternal_withValidToken_shouldSetAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + testToken);

        when(jwtService.isTokenValid(testToken)).thenReturn(true);
        when(jwtService.isAccessToken(testToken)).thenReturn(true);
        when(accessTokenBlacklist.isBlacklisted(testToken)).thenReturn(false);
        when(jwtService.extractUsername(testToken)).thenReturn("testuser@example.com");
        when(jwtService.extractRoles(testToken)).thenReturn(Arrays.asList("ROLE_CUSTOMER", "ROLE_ADMIN"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("testuser@example.com");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_ADMIN");
    }

    @Test
    void doFilterInternal_withBlacklistedToken_shouldNotSetAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + testToken);

        when(jwtService.isTokenValid(testToken)).thenReturn(true);
        when(jwtService.isAccessToken(testToken)).thenReturn(true);
        when(accessTokenBlacklist.isBlacklisted(testToken)).thenReturn(true);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_withoutAuthorizationHeader_shouldNotSetAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_withInvalidToken_shouldNotSetAuthentication() throws Exception {
        String invalidToken = "invalid.token.here";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + invalidToken);

        when(jwtService.isTokenValid(invalidToken)).thenReturn(false);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_withMalformedAuthorizationHeader_shouldNotExtractToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "InvalidFormat");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
