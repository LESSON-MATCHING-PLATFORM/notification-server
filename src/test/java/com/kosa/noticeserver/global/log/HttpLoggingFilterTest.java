package com.kosa.noticeserver.global.log;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class HttpLoggingFilterTest {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final HttpLoggingFilter filter = new HttpLoggingFilter();

    @Test
    void doFilterInternal_usesRequestIdHeaderAndCleansMdc() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/notification/token");
        request.addHeader(REQUEST_ID_HEADER, "request-001");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getHeader(REQUEST_ID_HEADER)).isEqualTo("request-001");
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void doFilterInternal_generatesRequestIdWhenHeaderIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/notification/token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getHeader(REQUEST_ID_HEADER)).isNotBlank();
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void doFilterInternal_excludesActuatorRequests() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getHeader(REQUEST_ID_HEADER)).isNull();
    }
}
