package com.yourorg.saascore.adapters.in.rest;

import com.yourorg.saascore.application.abac.AbacContext;
import com.yourorg.saascore.application.abac.AbacEvaluator;
import com.yourorg.saascore.application.abac.AbacResult;
import com.yourorg.saascore.config.TenantContext;
import com.yourorg.saascore.adapters.out.persistence.*;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/admin")
@PreAuthorize("hasAuthority('admin:read') or hasAuthority('admin:write') or hasAuthority('ROLE_admin')")
public class AdminController {

    private final AbacPolicyJpaRepository policyRepo;
    private final FeatureFlagJpaRepository featureFlagRepo;
    private final ChaosConfigJpaRepository chaosRepo;
    private final AbacEvaluator abacEvaluator;

    public AdminController(
            AbacPolicyJpaRepository policyRepo,
            FeatureFlagJpaRepository featureFlagRepo,
            ChaosConfigJpaRepository chaosRepo,
            AbacEvaluator abacEvaluator) {
        this.policyRepo = policyRepo;
        this.featureFlagRepo = featureFlagRepo;
        this.chaosRepo = chaosRepo;
        this.abacEvaluator = abacEvaluator;
    }

    private ResponseEntity<?> abacDeny() {
        return ResponseEntity.status(403).build();
    }

    @GetMapping("/policies")
    public ResponseEntity<?> listPolicies() {
        AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("admin:read"));
        if (!abac.allowed()) return abacDeny();
        UUID tenantId = TenantContext.getTenantId().orElse(null);
        if (tenantId == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(policyRepo.findByTenantId(tenantId));
    }

    @PutMapping("/policies/{id}")
    @PreAuthorize("hasAuthority('admin:write') or hasAuthority('ROLE_admin')")
    public ResponseEntity<?> updatePolicy(@PathVariable UUID id, @RequestBody AbacPolicyEntity body) {
        AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("admin:write"));
        if (!abac.allowed()) return abacDeny();
        UUID tenantId = TenantContext.getTenantId().orElse(null);
        if (tenantId == null) return ResponseEntity.badRequest().build();
        return policyRepo.findByIdAndTenantId(id, tenantId)
                .map(
                        existing -> {
                            if (body.getEffect() != null) existing.setEffect(body.getEffect());
                            if (body.getConditionsJson() != null) existing.setConditionsJson(body.getConditionsJson());
                            existing.setEnabled(body.isEnabled());
                            return ResponseEntity.ok(policyRepo.save(existing));
                        })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/feature-flags")
    public ResponseEntity<?> listFeatureFlags() {
        AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("admin:read"));
        if (!abac.allowed()) return abacDeny();
        UUID tenantId = TenantContext.getTenantId().orElse(null);
        if (tenantId == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(featureFlagRepo.findByTenantId(tenantId));
    }

    @PutMapping("/feature-flags/{name}")
    @PreAuthorize("hasAuthority('admin:write') or hasAuthority('ROLE_admin')")
    public ResponseEntity<?> updateFeatureFlag(@PathVariable String name, @RequestBody FeatureFlagEntity body) {
        AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("admin:write"));
        if (!abac.allowed()) return abacDeny();
        UUID tenantId = TenantContext.getTenantId().orElse(null);
        if (tenantId == null) return ResponseEntity.badRequest().build();
        return featureFlagRepo.findByTenantIdAndName(tenantId, name)
                .map(
                        existing -> {
                            existing.setEnabled(body.isEnabled());
                            existing.setRolloutPercentage(body.getRolloutPercentage());
                            if (body.getTargetingJson() != null) existing.setTargetingJson(body.getTargetingJson());
                            return ResponseEntity.ok(featureFlagRepo.save(existing));
                        })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/chaos")
    public ResponseEntity<?> getChaos() {
        AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("admin:read"));
        if (!abac.allowed()) return abacDeny();
        UUID tenantId = TenantContext.getTenantId().orElse(null);
        if (tenantId == null) return ResponseEntity.badRequest().build();
        return chaosRepo.findActiveByTenantId(tenantId, java.time.Instant.now())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PutMapping("/chaos")
    @PreAuthorize("hasAuthority('admin:write') or hasAuthority('ROLE_admin')")
    public ResponseEntity<?> setChaos(@RequestBody ChaosConfigEntity body) {
        AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("admin:write"));
        if (!abac.allowed()) return abacDeny();
        UUID tenantId = TenantContext.getTenantId().orElse(null);
        if (tenantId == null) return ResponseEntity.badRequest().build();
        body.setTenantId(tenantId);
        if (body.getExpires_at() == null) {
            body.setExpires_at(java.time.Instant.now().plusSeconds(3600));
        }
        return ResponseEntity.ok(chaosRepo.save(body));
    }
}
