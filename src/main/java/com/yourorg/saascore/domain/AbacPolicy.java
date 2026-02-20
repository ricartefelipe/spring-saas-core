package com.yourorg.saascore.domain;

import java.util.UUID;

public class AbacPolicy {

    private final UUID id;
    private final UUID tenantId;
    private final String permissionCode;
    private final Effect effect;
    private final int priority;
    private final boolean enabled;
    private final String conditionsJson;

    public AbacPolicy(
            UUID id,
            UUID tenantId,
            String permissionCode,
            Effect effect,
            int priority,
            boolean enabled,
            String conditionsJson) {
        this.id = id;
        this.tenantId = tenantId;
        this.permissionCode = permissionCode;
        this.effect = effect;
        this.priority = priority;
        this.enabled = enabled;
        this.conditionsJson = conditionsJson;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public Effect getEffect() {
        return effect;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getConditionsJson() {
        return conditionsJson;
    }

    public enum Effect {
        ALLOW,
        DENY
    }
}
