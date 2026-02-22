package com.union.solutions.saascore.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.union.solutions.saascore.adapters.out.persistence.PolicyEntity;
import com.union.solutions.saascore.adapters.out.persistence.PolicyJpaRepository;
import com.union.solutions.saascore.application.abac.AuditLogger;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.Policy;
import io.micrometer.core.instrument.Counter;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyService {

    private final PolicyJpaRepository repo;
    private final AuditLogger auditLogger;
    private final ObjectMapper objectMapper;
    private final Counter policiesUpdatedCounter;

    public PolicyService(PolicyJpaRepository repo, AuditLogger auditLogger,
                         ObjectMapper objectMapper,
                         @Qualifier("policiesUpdatedCounter") Counter policiesUpdatedCounter) {
        this.repo = repo;
        this.auditLogger = auditLogger;
        this.objectMapper = objectMapper;
        this.policiesUpdatedCounter = policiesUpdatedCounter;
    }

    @Transactional
    public PolicyEntity create(String permissionCode, Policy.Effect effect,
                               List<String> allowedPlans, List<String> allowedRegions,
                               boolean enabled, String notes) {
        PolicyEntity entity = new PolicyEntity();
        entity.setId(UUID.randomUUID());
        entity.setPermissionCode(permissionCode);
        entity.setEffect(effect);
        entity.setAllowedPlans(toJson(allowedPlans));
        entity.setAllowedRegions(toJson(allowedRegions));
        entity.setEnabled(enabled);
        entity.setNotes(notes);
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        repo.save(entity);
        auditLogger.log(TenantContext.getTenantId().orElse(null), TenantContext.getSubject(),
                TenantContext.getRoles().toString(), TenantContext.getPerms().toString(),
                "POLICY_CREATED", "policy", entity.getId().toString(),
                null, null, 201, TenantContext.getCorrelationId(), null);
        return entity;
    }

    @Transactional(readOnly = true)
    public Page<PolicyEntity> search(String permissionCode, Policy.Effect effect, Boolean enabled, Pageable pageable) {
        return repo.search(permissionCode, effect, enabled, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<PolicyEntity> getById(UUID id) {
        return repo.findById(id);
    }

    @Transactional
    public Optional<PolicyEntity> update(UUID id, String permissionCode, Policy.Effect effect,
                                         List<String> allowedPlans, List<String> allowedRegions,
                                         Boolean enabled, String notes) {
        return repo.findById(id).map(entity -> {
            if (permissionCode != null) entity.setPermissionCode(permissionCode);
            if (effect != null) entity.setEffect(effect);
            if (allowedPlans != null) entity.setAllowedPlans(toJson(allowedPlans));
            if (allowedRegions != null) entity.setAllowedRegions(toJson(allowedRegions));
            if (enabled != null) entity.setEnabled(enabled);
            if (notes != null) entity.setNotes(notes);
            entity.setUpdatedAt(Instant.now());
            repo.save(entity);
            policiesUpdatedCounter.increment();
            auditLogger.log(TenantContext.getTenantId().orElse(null), TenantContext.getSubject(),
                    TenantContext.getRoles().toString(), TenantContext.getPerms().toString(),
                    "POLICY_UPDATED", "policy", id.toString(),
                    null, null, 200, TenantContext.getCorrelationId(), null);
            return entity;
        });
    }

    @Transactional
    public boolean delete(UUID id) {
        if (repo.existsById(id)) {
            repo.deleteById(id);
            auditLogger.log(TenantContext.getTenantId().orElse(null), TenantContext.getSubject(),
                    TenantContext.getRoles().toString(), TenantContext.getPerms().toString(),
                    "POLICY_DELETED", "policy", id.toString(),
                    null, null, 204, TenantContext.getCorrelationId(), null);
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<PolicyEntity> getApplicablePolicies(String plan, String region) {
        return repo.findByEnabledTrue().stream()
                .filter(p -> {
                    List<String> plans = parseJson(p.getAllowedPlans());
                    List<String> regions = parseJson(p.getAllowedRegions());
                    boolean planMatch = plans.isEmpty() || plans.contains(plan);
                    boolean regionMatch = regions.isEmpty() || regions.contains(region);
                    return planMatch && regionMatch;
                })
                .toList();
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list != null ? list : List.of());
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> parseJson(String json) {
        if (json == null || json.isBlank() || "[]".equals(json)) return List.of();
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
