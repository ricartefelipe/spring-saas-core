package com.union.solutions.saascore.unit.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.observability.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  private final CorrelationIdFilter filter = new CorrelationIdFilter();

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void existingHeader_isPreserved() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Correlation-Id", "my-correlation-123");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("my-correlation-123");
  }

  @Test
  void missingHeader_generatesUuid() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    String generated = response.getHeader("X-Correlation-Id");
    assertThat(generated).isNotNull().isNotBlank();
    assertThat(generated).matches("[0-9a-f\\-]{36}");
  }

  @Test
  void blankHeader_generatesNewUuid() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Correlation-Id", "   ");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    String generated = response.getHeader("X-Correlation-Id");
    assertThat(generated).isNotNull().doesNotContain("   ");
  }

  @Test
  void mdcIsCleared_afterFilterCompletes() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(MDC.get("correlationId")).isNull();
    assertThat(MDC.get("tenantId")).isNull();
  }

  @Test
  void correlationIdSetInTenantContext_duringChainExecution() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Correlation-Id", "ctx-check-123");
    MockHttpServletResponse response = new MockHttpServletResponse();

    String[] capturedId = new String[1];
    FilterChain capturingChain =
        (ServletRequest req, ServletResponse res) ->
            capturedId[0] = TenantContext.getCorrelationId();

    filter.doFilter(request, response, capturingChain);

    assertThat(capturedId[0]).isEqualTo("ctx-check-123");
  }
}
