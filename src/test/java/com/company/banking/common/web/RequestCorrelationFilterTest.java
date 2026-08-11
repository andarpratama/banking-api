package com.company.banking.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generatesRequestIdWhenHeaderAbsent() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuringChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> {
            mdcDuringChain.set(MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID));
        });

        String headerId = response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER);
        assertThat(headerId).isNotBlank();
        assertThat(mdcDuringChain.get()).isEqualTo(headerId);
        assertThat(MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID)).isNull();
    }

    @Test
    void reusesSafeIncomingRequestId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "client-req-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER))
                .isEqualTo("client-req-123");
    }

    @Test
    void rejectsUnsafeIncomingRequestId() {
        assertThat(RequestCorrelationFilter.resolveRequestId("bad id with spaces"))
                .isNotEqualTo("bad id with spaces")
                .matches("^[0-9a-f\\-]{36}$");
        assertThat(RequestCorrelationFilter.resolveRequestId("x\".inject"))
                .doesNotContain("\"");
    }
}
