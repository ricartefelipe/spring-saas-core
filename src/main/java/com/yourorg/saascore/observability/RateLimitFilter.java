package com.yourorg.saascore.observability;

import com.yourorg.saascore.application.ratelimit.RateLimitService;
import com.yourorg.saascore.config.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RETRY_AFTER = "Retry-After";

    private final RateLimitService rateLimitService;
    private final int limitFree;
    private final int limitPro;
    private final int limitEnterprise;

    public RateLimitFilter(
            RateLimitService rateLimitService,
            @Value("${app.rate-limit.free:10}") int limitFree,
            @Value("${app.rate-limit.pro:50}") int limitPro,
            @Value("${app.rate-limit.enterprise:200}") int limitEnterprise) {
        this.rateLimitService = rateLimitService;
        this.limitFree = limitFree;
        this.limitPro = limitPro;
        this.limitEnterprise = limitEnterprise;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        String tenantKey = TenantContext.getTenantId().map(Object::toString).orElse("anonymous");
        String plan = TenantContext.getPlan();
        int limit = limitForPlan(plan);
        String endpointGroup = request.getMethod().equals("GET") ? "read" : "write";
        String key = tenantKey + ":" + endpointGroup;

        RateLimitService.RateLimitResult result = rateLimitService.tryConsume(key, limit);
        response.setHeader(HEADER_LIMIT, String.valueOf(result.limit()));
        response.setHeader(HEADER_REMAINING, String.valueOf(result.remaining()));
        if (result.retryAfterSeconds() > 0) {
            response.setHeader(HEADER_RETRY_AFTER, String.valueOf(result.retryAfterSeconds()));
        }
        if (!result.allowed()) {
            response.setStatus(429);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private int limitForPlan(String plan) {
        if (plan == null) return limitFree;
        return switch (plan) {
            case "pro" -> limitPro;
            case "enterprise" -> limitEnterprise;
            default -> limitFree;
        };
    }
}
