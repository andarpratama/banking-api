package com.company.banking.security;

import com.company.banking.common.constants.ErrorCode;
import com.company.banking.common.response.ErrorResponse;
import com.company.banking.config.RateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Applies auth-path and global fixed-window rate limits; returns 429 when exceeded.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    public static final String HEADER_LIMIT = "X-RateLimit-Limit";
    public static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_RESET = "X-RateLimit-Reset";
    public static final String HEADER_RETRY_AFTER = "Retry-After";

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(
            RateLimiter rateLimiter,
            RateLimitProperties properties,
            ObjectMapper objectMapper
    ) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.objectMapper = objectMapper.copy()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        return path.equals("/api/v1/health")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/v3/api-docs")
                || path.equals("/actuator/prometheus")
                || path.equals("/actuator/health")
                || path.startsWith("/actuator/health/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean authPath = path != null && path.startsWith("/api/v1/auth/");
        String clientKey = resolveClientKey(request);

        int limit = authPath ? properties.getAuthLimit() : properties.getGlobalLimit();
        int windowSeconds = authPath
                ? properties.getAuthWindowSeconds()
                : properties.getGlobalWindowSeconds();
        String bucket = (authPath ? "auth:" : "global:") + clientKey;

        RateLimitResult result = rateLimiter.tryConsume(bucket, limit, windowSeconds);
        writeRateLimitHeaders(response, result);

        if (!result.allowed()) {
            writeTooManyRequests(request, response, result);
            return;
        }

        filterChain.doFilter(request, response);
    }

    static String resolveClientKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getName() != null
                && !authentication.getName().isBlank()) {
            return "user:" + authentication.getName();
        }
        return "ip:" + clientIp(request);
    }

    static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? "unknown" : remote;
    }

    private static void writeRateLimitHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader(HEADER_LIMIT, String.valueOf(result.limit()));
        response.setHeader(HEADER_REMAINING, String.valueOf(result.remaining()));
        response.setHeader(HEADER_RESET, String.valueOf(result.resetEpochSeconds()));
    }

    private void writeTooManyRequests(
            HttpServletRequest request,
            HttpServletResponse response,
            RateLimitResult result
    ) throws IOException {
        long retryAfter = Math.max(1L, result.resetEpochSeconds() - Instant.now().getEpochSecond());
        response.setHeader(HEADER_RETRY_AFTER, String.valueOf(retryAfter));
        response.setStatus(ErrorCode.RATE_LIMIT_EXCEEDED.getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                ErrorCode.RATE_LIMIT_EXCEEDED.getHttpStatus(),
                ErrorCode.RATE_LIMIT_EXCEEDED.name(),
                "Rate limit exceeded. Try again later.",
                request.getRequestURI()
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
