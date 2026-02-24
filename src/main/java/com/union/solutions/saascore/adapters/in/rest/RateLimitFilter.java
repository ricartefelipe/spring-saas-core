package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.config.TenantContext;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Order(10)
@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private final RateLimiterRegistry registry;
  private final Map<String, Integer> planLimits;

  public RateLimitFilter(
      @Value("${app.rate-limit.free:60}") int freeLimit,
      @Value("${app.rate-limit.pro:300}") int proLimit,
      @Value("${app.rate-limit.enterprise:1000}") int enterpriseLimit) {
    this.registry = RateLimiterRegistry.ofDefaults();
    this.planLimits = Map.of("free", freeLimit, "pro", proLimit, "enterprise", enterpriseLimit);
  }

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/healthz")
        || path.startsWith("/readyz")
        || path.startsWith("/actuator")
        || path.startsWith("/docs")
        || path.startsWith("/v3/api-docs")
        || path.startsWith("/v1/dev/");
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String plan = TenantContext.getPlan();
    String tenantKey = TenantContext.getTenantId().map(Object::toString).orElse("anonymous");

    if (plan == null || plan.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }

    int limit = planLimits.getOrDefault(plan, planLimits.getOrDefault("free", 60));
    String key = tenantKey + ":" + plan;

    RateLimiter rateLimiter =
        registry.rateLimiter(
            key,
            RateLimiterConfig.custom()
                .limitForPeriod(limit)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO)
                .build());

    if (rateLimiter.acquirePermission()) {
      long remaining = Math.max(0, rateLimiter.getMetrics().getAvailablePermissions());
      response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
      response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
      filterChain.doFilter(request, response);
    } else {
      response.setStatus(429);
      response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
      response.setHeader("X-RateLimit-Remaining", "0");
      response.setHeader("Retry-After", "60");
      response.setContentType("application/json");
      response
          .getWriter()
          .write(
              "{\"status\":429,\"title\":\"Too Many Requests\","
                  + "\"detail\":\"Rate limit exceeded for plan: "
                  + plan
                  + "\"}");
    }
  }
}
