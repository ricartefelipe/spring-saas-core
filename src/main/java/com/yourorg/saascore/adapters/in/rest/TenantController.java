package com.yourorg.saascore.adapters.in.rest;

import com.yourorg.saascore.application.tenant.TenantResolver;
import com.yourorg.saascore.application.tenant.TenantUseCase;
import com.yourorg.saascore.config.TenantContext;
import com.yourorg.saascore.domain.Tenant;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/tenants")
public class TenantController {

    private final TenantUseCase tenantUseCase;
    private final TenantResolver tenantResolver;

    public TenantController(TenantUseCase tenantUseCase, TenantResolver tenantResolver) {
        this.tenantUseCase = tenantUseCase;
        this.tenantResolver = tenantResolver;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('tenants:write') or hasAuthority('ROLE_admin')")
    public ResponseEntity<TenantDto> create(@Valid @RequestBody CreateTenantRequest request) {
        Tenant t =
                tenantUseCase.create(
                        request.getName(),
                        request.getPlan(),
                        request.getPrimaryRegion(),
                        request.getShardKey());
        return ResponseEntity.status(201).body(TenantDto.from(t));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TenantDto> getById(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Tenant-Id", required = false) String headerTenantId) {
        UUID tidFromToken = TenantContext.getTenantId().orElse(null);
        if (tidFromToken != null && headerTenantId != null && !tidFromToken.toString().equals(headerTenantId)) {
            return ResponseEntity.status(403).build();
        }
        UUID scopeTenant = null;
        if (headerTenantId != null) {
            try {
                scopeTenant = UUID.fromString(headerTenantId);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            scopeTenant = tidFromToken;
        }
        if (scopeTenant != null) {
            tenantResolver.resolveAndSetContext(scopeTenant);
        }
        return tenantUseCase.getById(id)
                .map(t -> ResponseEntity.ok(TenantDto.from(t)))
                .orElse(ResponseEntity.notFound().build());
    }
}
