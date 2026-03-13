package com.union.solutions.saascore.infrastructure.tracing;

import com.union.solutions.saascore.config.TenantContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Order(1)
@Component
@ConditionalOnBean(Tracer.class)
public class TracingFilter extends OncePerRequestFilter {

  private final Tracer tracer;

  public TracingFilter(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    Span currentSpan = tracer.currentSpan();
    if (currentSpan != null) {
      enrichSpanWithContext(currentSpan);
      propagateTraceToMdc(currentSpan);
      response.setHeader("X-Trace-Id", currentSpan.context().traceId());
    }
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove("traceId");
      MDC.remove("spanId");
    }
  }

  private void enrichSpanWithContext(Span span) {
    String correlationId = TenantContext.getCorrelationId();
    if (correlationId != null && !correlationId.isBlank()) {
      span.tag("correlation.id", correlationId);
    }
    TenantContext.getTenantId().ifPresent(tid -> span.tag("tenant.id", tid.toString()));
  }

  private void propagateTraceToMdc(Span span) {
    MDC.put("traceId", span.context().traceId());
    MDC.put("spanId", span.context().spanId());
  }
}
