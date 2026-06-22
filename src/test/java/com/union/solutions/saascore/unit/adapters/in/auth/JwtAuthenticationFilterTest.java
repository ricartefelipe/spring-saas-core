package com.union.solutions.saascore.unit.adapters.in.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.union.solutions.saascore.adapters.in.auth.JwtAuthenticationFilter;
import com.union.solutions.saascore.adapters.in.auth.TokenClaims;
import com.union.solutions.saascore.adapters.in.auth.TokenParseResult;
import com.union.solutions.saascore.application.port.AuditLogger;
import com.union.solutions.saascore.config.TenantContext;
import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class JwtAuthenticationFilterTest {

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void ignoresTenantHeaderWhenJwtHasNoTenantClaim() throws Exception {
    UUID headerTenant = UUID.randomUUID();
    JwtAuthenticationFilter filter =
        new JwtAuthenticationFilter(
            token ->
                Optional.of(
                    TokenParseResult.current(
                        new TokenClaims(
                            "user@test.com",
                            null,
                            List.of("admin"),
                            List.of("users:read"),
                            "pro",
                            "us"))),
            mock(AuditLogger.class));
    MockHttpServletRequest request = requestWithToken();
    request.addHeader("X-Tenant-Id", headerTenant.toString());
    MockHttpServletResponse response = new MockHttpServletResponse();
    UUID[] capturedTenant = new UUID[1];

    FilterChain chain = (req, res) -> capturedTenant[0] = TenantContext.getTenantId().orElse(null);

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(capturedTenant[0]).isNull();
  }

  @Test
  void usesTenantHeaderWhenJwtHasPlatformWildcardClaim() throws Exception {
    UUID headerTenant = UUID.randomUUID();
    JwtAuthenticationFilter filter =
        new JwtAuthenticationFilter(
            token ->
                Optional.of(
                    TokenParseResult.current(
                        new TokenClaims(
                            "admin@local",
                            "*",
                            List.of("admin"),
                            List.of("flags:read"),
                            "pro",
                            "us"))),
            mock(AuditLogger.class));
    MockHttpServletRequest request = requestWithToken();
    request.addHeader("X-Tenant-Id", headerTenant.toString());
    MockHttpServletResponse response = new MockHttpServletResponse();
    UUID[] capturedTenant = new UUID[1];

    FilterChain chain = (req, res) -> capturedTenant[0] = TenantContext.getTenantId().orElse(null);

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(capturedTenant[0]).isEqualTo(headerTenant);
  }

  @Test
  void rejectsTenantHeaderThatDiffersFromJwtTenantClaim() throws Exception {
    UUID jwtTenant = UUID.randomUUID();
    UUID headerTenant = UUID.randomUUID();
    JwtAuthenticationFilter filter =
        new JwtAuthenticationFilter(
            token ->
                Optional.of(
                    TokenParseResult.current(
                        new TokenClaims(
                            "user@test.com",
                            jwtTenant.toString(),
                            List.of("admin"),
                            List.of("users:read"),
                            "pro",
                            "us"))),
            mock(AuditLogger.class));
    MockHttpServletRequest request = requestWithToken();
    request.addHeader("X-Tenant-Id", headerTenant.toString());
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(403);
  }

  private MockHttpServletRequest requestWithToken() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/users");
    request.addHeader("Authorization", "Bearer token");
    return request;
  }
}
