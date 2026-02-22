package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.config.TenantContext;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
public class MeController {

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication auth) {
        String sub = auth != null ? auth.getName() : "";
        return ResponseEntity.ok(Map.of(
                "sub", sub,
                "tenant_id", TenantContext.getTenantId().map(Object::toString).orElse(""),
                "plan", TenantContext.getPlan(),
                "region", TenantContext.getRegion(),
                "roles", TenantContext.getRoles(),
                "perms", TenantContext.getPerms(),
                "correlation_id", TenantContext.getCorrelationId() != null ? TenantContext.getCorrelationId() : ""));
    }
}
