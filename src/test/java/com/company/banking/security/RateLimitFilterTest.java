package com.company.banking.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.banking.config.RateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private RateLimitProperties properties;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.setBackend("memory");
        properties.setAuthLimit(2);
        properties.setAuthWindowSeconds(60);
        properties.setGlobalLimit(100);
        properties.setGlobalWindowSeconds(60);
        filter = new RateLimitFilter(new InMemoryRateLimiter(), properties, new ObjectMapper());
    }

    @Test
    void authPathReturns429WhenLimitExceeded() throws ServletException, IOException {
        MockHttpServletResponse first = exchange("/api/v1/auth/login");
        MockHttpServletResponse second = exchange("/api/v1/auth/login");
        MockHttpServletResponse third = exchange("/api/v1/auth/login");

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(200);
        assertThat(third.getStatus()).isEqualTo(429);
        assertThat(third.getHeader(RateLimitFilter.HEADER_LIMIT)).isEqualTo("2");
        assertThat(third.getHeader(RateLimitFilter.HEADER_REMAINING)).isEqualTo("0");
        assertThat(third.getHeader(RateLimitFilter.HEADER_RESET)).isNotBlank();
        assertThat(third.getHeader(RateLimitFilter.HEADER_RETRY_AFTER)).isNotBlank();
        assertThat(third.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void healthIsExcludedFromRateLimiting() throws ServletException, IOException {
        properties.setAuthLimit(1);
        properties.setGlobalLimit(1);
        filter = new RateLimitFilter(new InMemoryRateLimiter(), properties, new ObjectMapper());

        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = exchange("/api/v1/health");
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getHeader(RateLimitFilter.HEADER_LIMIT)).isNull();
        }
    }

    @Test
    void successfulResponsesIncludeRateLimitHeaders() throws ServletException, IOException {
        MockHttpServletResponse response = exchange("/api/v1/accounts");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader(RateLimitFilter.HEADER_LIMIT)).isEqualTo("100");
        assertThat(response.getHeader(RateLimitFilter.HEADER_REMAINING)).isEqualTo("99");
        assertThat(response.getHeader(RateLimitFilter.HEADER_RESET)).isNotBlank();
    }

    private MockHttpServletResponse exchange(String path) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr("203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        if (response.getStatus() == 200 && response.getContentAsByteArray().length == 0) {
            // MockFilterChain leaves status 200 by default when filter continues.
            response.setStatus(200);
        }
        return response;
    }
}
