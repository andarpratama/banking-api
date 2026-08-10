package com.company.banking.auth.application;

import com.company.banking.audit.application.AuditPayloadHasher;
import com.company.banking.audit.application.AuditService;
import com.company.banking.audit.application.RecordAuditCommand;
import com.company.banking.audit.domain.AuditActions;
import com.company.banking.audit.domain.AuditStatus;
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
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String TOKEN_TYPE_BEARER = "Bearer";

    private final UserAccountRepository users;
    private final CustomerProfileRepository customers;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AccessTokenBlacklist accessTokenBlacklist;
    private final AuditService auditService;

    public AuthService(
            UserAccountRepository users,
            CustomerProfileRepository customers,
            RefreshTokenRepository refreshTokens,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            AccessTokenBlacklist accessTokenBlacklist,
            AuditService auditService
    ) {
        this.users = users;
        this.customers = customers;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.accessTokenBlacklist = accessTokenBlacklist;
        this.auditService = auditService;
    }

    @Transactional
    public RegisterResponse register(
            String email,
            String password,
            String fullName,
            String phone,
            String address
    ) {
        String normalizedEmail = email.trim().toLowerCase();
        if (users.existsByEmail(normalizedEmail)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL, "Email already registered");
        }

        String passwordHash = passwordEncoder.encode(password);
        UserAccount user = users.save(normalizedEmail, passwordHash, ROLE_CUSTOMER);
        CustomerProfile customer = customers.create(user.id(), fullName.trim(), phone, address);

        auditService.record(RecordAuditCommand.of(
                user.email(),
                "/auth/register",
                "POST",
                AuditActions.REGISTER,
                201,
                null,
                AuditPayloadHasher.sha256("userId=" + user.id() + ";customerId=" + customer.id())
        ));

        return new RegisterResponse(
                user.id().toString(),
                user.email(),
                customer.fullName(),
                customer.id().toString(),
                customer.createdAt()
        );
    }

    @Transactional
    public LoginResponse login(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase();
        UserAccount user = users.findByEmail(normalizedEmail)
                .filter(UserAccount::enabled)
                .orElse(null);

        if (user == null || !passwordEncoder.matches(password, user.passwordHash())) {
            auditLoginFailure(normalizedEmail);
            throw invalidCredentials();
        }

        IssuedTokens tokens = issueTokens(user);
        auditService.record(RecordAuditCommand.of(
                user.email(),
                "/auth/login",
                "POST",
                AuditActions.LOGIN,
                200,
                null,
                AuditPayloadHasher.sha256("userId=" + user.id())
        ));

        return new LoginResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                TOKEN_TYPE_BEARER,
                jwtProperties.getAccessTokenExpiration(),
                new LoginResponse.UserInfo(
                        user.id().toString(),
                        user.email(),
                        List.copyOf(user.roles())
                )
        );
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw invalidToken();
        }

        if (!jwtService.isTokenValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw invalidToken();
        }

        String hash = TokenHasher.sha256(refreshToken);
        RefreshTokenRecord stored = refreshTokens.findByTokenHash(hash)
                .orElseThrow(this::invalidToken);

        Instant now = Instant.now();
        if (!stored.isUsable(now)) {
            throw invalidToken();
        }

        UserAccount user = users.findById(stored.userId())
                .filter(UserAccount::enabled)
                .orElseThrow(this::invalidToken);

        refreshTokens.revoke(stored.id());
        IssuedTokens tokens = issueTokens(user);

        return new TokenResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                TOKEN_TYPE_BEARER,
                jwtProperties.getAccessTokenExpiration()
        );
    }

    @Transactional
    public void logout(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Missing or invalid authorization");
        }

        if (!jwtService.isTokenValid(accessToken) || !jwtService.isAccessToken(accessToken)) {
            throw invalidAccessToken();
        }

        if (accessTokenBlacklist.isBlacklisted(accessToken)) {
            return;
        }

        String email = jwtService.extractUsername(accessToken);
        UserAccount user = users.findByEmail(email).orElse(null);

        Instant expiresAt = jwtService.extractExpiration(accessToken);
        accessTokenBlacklist.blacklist(accessToken, expiresAt);

        if (user != null) {
            refreshTokens.revokeAllForUser(user.id());
            auditService.record(RecordAuditCommand.of(
                    user.email(),
                    "/auth/logout",
                    "POST",
                    AuditActions.LOGOUT,
                    200,
                    null,
                    AuditPayloadHasher.sha256("userId=" + user.id())
            ));
        }
    }

    private void auditLoginFailure(String email) {
        auditService.record(new RecordAuditCommand(
                email,
                "/auth/login",
                "POST",
                AuditActions.LOGIN,
                401,
                AuditStatus.FAILURE,
                null,
                AuditPayloadHasher.sha256("email=" + email)
        ));
    }

    private IssuedTokens issueTokens(UserAccount user) {
        UserDetails principal = toUserDetails(user);
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        Instant refreshExpiresAt = Instant.now().plusSeconds(jwtProperties.getRefreshTokenExpiration());
        refreshTokens.save(user.id(), TokenHasher.sha256(refreshToken), refreshExpiresAt);

        return new IssuedTokens(accessToken, refreshToken);
    }

    private static UserDetails toUserDetails(UserAccount user) {
        List<SimpleGrantedAuthority> authorities = user.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        return User.builder()
                .username(user.email())
                .password(user.passwordHash())
                .disabled(!user.enabled())
                .authorities(authorities)
                .build();
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
    }

    private BusinessException invalidToken() {
        return new BusinessException(ErrorCode.INVALID_TOKEN, "Refresh token expired or invalid");
    }

    private BusinessException invalidAccessToken() {
        return new BusinessException(ErrorCode.INVALID_TOKEN, "Access token expired or invalid");
    }

    private record IssuedTokens(String accessToken, String refreshToken) {
    }
}
