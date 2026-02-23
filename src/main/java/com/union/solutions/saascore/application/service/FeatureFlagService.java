package com.union.solutions.saascore.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.union.solutions.saascore.adapters.out.persistence.FeatureFlagEntity;
import com.union.solutions.saascore.adapters.out.persistence.FeatureFlagJpaRepository;
import com.union.solutions.saascore.application.abac.AuditLogger;
import com.union.solutions.saascore.config.TenantContext;
import io.micrometer.core.instrument.Counter;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeatureFlagService {

    private final FeatureFlagJpaRepository repo;
    private final AuditLogger auditLogger;
    private final ObjectMapper objectMapper;
    private final Counter flagsToggledCounter;

    public FeatureFlagService(FeatureFlagJpaRepository repo, AuditLogger auditLogger,
                              ObjectMapper objectMapper,
                              @Qualifier("flagsToggledCounter") Counter flagsToggledCounter) {
        this.repo = repo;
        this.auditLogger = auditLogger;
        this.objectMapper = objectMapper;
        this.flagsToggledCounter = flagsToggledCounter;
    }

    @Transactional
    public FeatureFlagEntity create(UUID tenantId, String name, boolean enabled,
                                    int rolloutPercent, List<String> allowedRoles) {
        if (repo.findByTenantIdAndName(tenantId, name).isPresent()) {
            throw new IllegalArgumentException("Flag '" + name + "' already exists for tenant");
        }
        FeatureFlagEntity entity = new FeatureFlagEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setName(name);
        entity.setEnabled(enabled);
        entity.setRolloutPercent(Math.max(0, Math.min(100, rolloutPercent)));
        entity.setAllowedRoles(toJson(allowedRoles));
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        repo.save(entity);
        auditLogger.log(tenantId, TenantContext.getSubject(),
                TenantContext.getRoles().toString(), TenantContext.getPerms().toString(),
                "FLAG_CREATED", "feature_flag", name,
                null, null, 201, TenantContext.getCorrelationId(), null);
        return entity;
    }

    @Transactional(readOnly = true)
    public List<FeatureFlagEntity> listByTenant(UUID tenantId) {
        return repo.findByTenantId(tenantId);
    }

    @Transactional
    public Optional<FeatureFlagEntity> update(UUID tenantId, String name,
                                              Boolean enabled, Integer rolloutPercent,
                                              List<String> allowedRoles) {
        return repo.findByTenantIdAndName(tenantId, name).map(entity -> {
            if (enabled != null) entity.setEnabled(enabled);
            if (rolloutPercent != null) entity.setRolloutPercent(Math.max(0, Math.min(100, rolloutPercent)));
            if (allowedRoles != null) entity.setAllowedRoles(toJson(allowedRoles));
            entity.setUpdatedAt(Instant.now());
            repo.save(entity);
            flagsToggledCounter.increment();
            auditLogger.log(tenantId, TenantContext.getSubject(),
                    TenantContext.getRoles().toString(), TenantContext.getPerms().toString(),
                    "FLAG_UPDATED", "feature_flag", name,
                    null, null, 200, TenantContext.getCorrelationId(), null);
            return entity;
        });
    }

    @Transactional
    public boolean delete(UUID tenantId, String name) {
        return repo.findByTenantIdAndName(tenantId, name).map(entity -> {
            repo.delete(entity);
            auditLogger.log(tenantId, TenantContext.getSubject(),
                    TenantContext.getRoles().toString(), TenantContext.getPerms().toString(),
                    "FLAG_DELETED", "feature_flag", name,
                    null, null, 204, TenantContext.getCorrelationId(), null);
            return true;
        }).orElse(false);
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list != null ? list : List.of());
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
