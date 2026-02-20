package com.yourorg.saascore.adapters.in.auth;

import com.yourorg.saascore.application.tenant.TenantResolver;
import com.yourorg.saascore.config.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Order(1)
@Component
public class TenantResolutionFilter extends OncePerRequestFilter {

    private final TenantResolver tenantResolver;

    public TenantResolutionFilter(TenantResolver tenantResolver) {
        this.tenantResolver = tenantResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        TenantContext.getTenantId().ifPresent(tenantResolver::resolveAndSetContext);
        filterChain.doFilter(request, response);
    }
}
