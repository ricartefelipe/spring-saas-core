package com.union.solutions.saascore.adapters.in.auth;

import com.union.solutions.saascore.application.abac.AuditLogger;
import com.union.solutions.saascore.config.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String ACTION_JWT_VERIFIED_PREVIOUS_KEY = "JWT_VERIFIED_WITH_PREVIOUS_KEY";

  private final TokenParser tokenParser;
  private final AuditLogger auditLogger;

  public JwtAuthenticationFilter(TokenParser tokenParser, AuditLogger auditLogger) {
    this.tokenParser = tokenParser;
    this.auditLogger = auditLogger;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String auth = request.getHeader("Authorization");
    if (auth != null && auth.startsWith("Bearer ")) {
      String token = auth.substring(7);
      tokenParser
          .parse(token)
          .ifPresent(
              parseResult -> {
                TokenClaims claims = parseResult.claims();
                if (parseResult.verifiedWithPreviousKey()) {
                  auditLogger.log(
                      parseUuidOrNull(claims.tid()),
                      claims.sub(),
                      claims.roles().toString(),
                      claims.perms().toString(),
                      ACTION_JWT_VERIFIED_PREVIOUS_KEY,
                      "jwt",
                      null,
                      request.getMethod(),
                      request.getRequestURI(),
                      200,
                      request.getHeader("X-Correlation-Id"),
                      "JWT verified with previous/rotated key during secret rotation");
                }
                String sub = claims.sub();
                String tid = claims.tid();
                String plan = claims.plan();
                String region = claims.region();
                List<String> roles = claims.roles();
                List<String> perms = claims.perms();

                List<SimpleGrantedAuthority> authorities =
                    Stream.concat(roles.stream().map(r -> "ROLE_" + r), perms.stream())
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(sub, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                TenantContext.setSubject(sub);
                TenantContext.setPlan(plan != null ? plan : "");
                TenantContext.setRegion(region != null ? region : "");
                TenantContext.setRoles(roles);
                TenantContext.setPerms(perms);

                String tenantHeader = request.getHeader("X-Tenant-Id");
                if (tid != null && !tid.isBlank()) {
                  try {
                    UUID tenantUuid = UUID.fromString(tid);
                    if (tenantHeader != null && !tenantHeader.isBlank()) {
                      UUID headerUuid = UUID.fromString(tenantHeader);
                      if (!tenantUuid.equals(headerUuid) && !"*".equals(tid)) {
                        response.setStatus(403);
                        return;
                      }
                    }
                    TenantContext.setTenantId(tenantUuid);
                  } catch (IllegalArgumentException ignored) {
                  }
                } else if (tenantHeader != null && !tenantHeader.isBlank()) {
                  try {
                    TenantContext.setTenantId(UUID.fromString(tenantHeader));
                  } catch (IllegalArgumentException ignored) {
                  }
                }

                TenantContext.getTenantId().ifPresent(t -> MDC.put("tenantId", t.toString()));
              });
    }
    try {
      filterChain.doFilter(request, response);
    } finally {
      TenantContext.clear();
    }
  }

  private static UUID parseUuidOrNull(String tid) {
    if (tid == null || tid.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(tid);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
