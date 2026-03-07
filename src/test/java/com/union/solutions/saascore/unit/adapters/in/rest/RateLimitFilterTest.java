package com.union.solutions.saascore.unit.adapters.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.union.solutions.saascore.adapters.in.rest.RateLimitFilter;
import com.union.solutions.saascore.config.TenantContext;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

  private RateLimitFilter filter;

  @BeforeEach
  void setUp() {
    filter = new RateLimitFilter(3, 5, 10);
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void freePlan_allowsUpToLimit_thenReturns429() throws Exception {
    TenantContext.setTenantId(UUID.randomUUID());
    TenantContext.setPlan("free");

    for (int i = 0; i < 3; i++) {
      MockHttpServletResponse resp = doFilter("/v1/tenants");
      assertThat(resp.getStatus()).isEqualTo(200);
      assertThat(resp.getHeader("X-RateLimit-Limit")).isEqualTo("3");
    }

    MockHttpServletResponse resp = doFilter("/v1/tenants");
    assertThat(resp.getStatus()).isEqualTo(429);
    assertThat(resp.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
    assertThat(resp.getHeader("Retry-After")).isEqualTo("60");
  }

  @Test
  void proPlan_allowsHigherLimit() throws Exception {
    TenantContext.setTenantId(UUID.randomUUID());
    TenantContext.setPlan("pro");

    for (int i = 0; i < 5; i++) {
      MockHttpServletResponse resp = doFilter("/v1/tenants");
      assertThat(resp.getStatus()).isEqualTo(200);
    }

    MockHttpServletResponse resp = doFilter("/v1/tenants");
    assertThat(resp.getStatus()).isEqualTo(429);
  }

  @Test
  void enterprisePlan_allowsHighestLimit() throws Exception {
    TenantContext.setTenantId(UUID.randomUUID());
    TenantContext.setPlan("enterprise");

    for (int i = 0; i < 10; i++) {
      MockHttpServletResponse resp = doFilter("/v1/tenants");
      assertThat(resp.getStatus()).isEqualTo(200);
    }

    MockHttpServletResponse resp = doFilter("/v1/tenants");
    assertThat(resp.getStatus()).isEqualTo(429);
  }

  @Test
  void noPlan_passesThrough() throws Exception {
    TenantContext.setTenantId(UUID.randomUUID());
    TenantContext.setPlan("");

    MockHttpServletResponse resp = doFilter("/v1/tenants");
    assertThat(resp.getStatus()).isEqualTo(200);
    assertThat(resp.getHeader("X-RateLimit-Limit")).isNull();
  }

  @Test
  void excludedPaths_areNotFiltered() throws Exception {
    TenantContext.setPlan("free");

    for (String path :
        new String[] {
          "/healthz", "/readyz", "/actuator/health", "/docs", "/v3/api-docs", "/v1/dev/token"
        }) {
      MockHttpServletResponse resp = doFilter(path);
      assertThat(resp.getHeader("X-RateLimit-Limit"))
          .as("Path %s should be excluded from rate limiting", path)
          .isNull();
      assertThat(resp.getStatus()).isEqualTo(200);
    }
  }

  @Test
  void unknownPlan_fallsBackToFreeLimit() throws Exception {
    TenantContext.setTenantId(UUID.randomUUID());
    TenantContext.setPlan("custom");

    for (int i = 0; i < 3; i++) {
      MockHttpServletResponse resp = doFilter("/v1/tenants");
      assertThat(resp.getStatus()).isEqualTo(200);
    }

    MockHttpServletResponse resp = doFilter("/v1/tenants");
    assertThat(resp.getStatus()).isEqualTo(429);
  }

  @Test
  void rateLimitHeaders_presentOnSuccess() throws Exception {
    TenantContext.setTenantId(UUID.randomUUID());
    TenantContext.setPlan("free");

    MockHttpServletResponse resp = doFilter("/v1/tenants");
    assertThat(resp.getHeader("X-RateLimit-Limit")).isEqualTo("3");
    assertThat(resp.getHeader("X-RateLimit-Remaining")).isNotNull();
  }

  private MockHttpServletResponse doFilter(String path) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    request.setServletPath(path);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(request, response, chain);
    return response;
  }
}
