package com.yourorg.saascore.adapters.in.auth;

import com.yourorg.saascore.config.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtParser jwtParser;

    public JwtAuthenticationFilter(JwtParser jwtParser) {
        this.jwtParser = jwtParser;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            jwtParser
                    .parse(token)
                    .ifPresent(
                            claims -> {
                                String sub = claims.getSubject();
                                String tid = claims.get("tid", String.class);
                                String region =
                                        Optional.ofNullable(claims.get("region", String.class))
                                                .orElse("region-a");
                                String plan = Optional.ofNullable(claims.get("plan", String.class)).orElse("free");
                                String jti = claims.get("jti", String.class);
                                @SuppressWarnings("unchecked")
                                List<String> roles =
                                        claims.get("roles") != null
                                                ? (List<String>) claims.get("roles")
                                                : Collections.emptyList();
                                @SuppressWarnings("unchecked")
                                List<String> perms =
                                        claims.get("perms") != null
                                                ? (List<String>) claims.get("perms")
                                                : Collections.emptyList();
                                List<SimpleGrantedAuthority> authorities =
                                        Stream.concat(
                                                        roles.stream().map(r -> "ROLE_" + r),
                                                        perms.stream())
                                                .map(SimpleGrantedAuthority::new)
                                                .toList();
                                UsernamePasswordAuthenticationToken authToken =
                                        new UsernamePasswordAuthenticationToken(
                                                sub, null, authorities);
                                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(authToken);
                                if (tid != null && !"*".equals(tid)) {
                                    try {
                                        TenantContext.setTenantId(UUID.fromString(tid));
                                    } catch (Exception ignored) {
                                    }
                                }
                                TenantContext.setRegion(region);
                                TenantContext.setPlan(plan);
                                TenantContext.setJti(jti);
                                TenantContext.setSubject(sub);
                                String corr = request.getHeader("X-Correlation-Id");
                                if (corr != null) TenantContext.setCorrelationId(corr);
                                String tenantHeader = request.getHeader("X-Tenant-Id");
                                if (tenantHeader != null && tid != null && tid.equals(tenantHeader)) {
                                    try {
                                        TenantContext.setTenantId(UUID.fromString(tenantHeader));
                                    } catch (Exception ignored) {
                                    }
                                }
                                String consistency = request.getHeader("Consistency");
                                if (consistency != null) TenantContext.setConsistency(consistency);
                            });
        }
        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId != null) {
            TenantContext.setCorrelationId(correlationId);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
