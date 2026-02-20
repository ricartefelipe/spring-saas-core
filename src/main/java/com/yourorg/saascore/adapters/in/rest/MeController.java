package com.yourorg.saascore.adapters.in.rest;

import com.yourorg.saascore.config.TenantContext;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class MeController {

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication auth) {
        String sub = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(
                Map.of(
                        "sub", sub != null ? sub : "",
                        "tenant_id", TenantContext.getTenantId().map(Object::toString).orElse(""),
                        "region", TenantContext.getRegion(),
                        "correlation_id", TenantContext.getCorrelationId() != null ? TenantContext.getCorrelationId() : ""));
    }
}
