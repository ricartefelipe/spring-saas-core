package com.union.solutions.saascore.config;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<Holder> HOLDER = ThreadLocal.withInitial(Holder::new);

    private TenantContext() {}

    public static void setTenantId(UUID tenantId) { HOLDER.get().tenantId = tenantId; }
    public static Optional<UUID> getTenantId() { return Optional.ofNullable(HOLDER.get().tenantId); }
    public static void setCorrelationId(String correlationId) { HOLDER.get().correlationId = correlationId; }
    public static String getCorrelationId() { return HOLDER.get().correlationId; }
    public static void setSubject(String subject) { HOLDER.get().subject = subject; }
    public static String getSubject() { return HOLDER.get().subject; }
    public static void setPlan(String plan) { HOLDER.get().plan = plan; }
    public static String getPlan() { return HOLDER.get().plan != null ? HOLDER.get().plan : ""; }
    public static void setRegion(String region) { HOLDER.get().region = region; }
    public static String getRegion() { return HOLDER.get().region != null ? HOLDER.get().region : ""; }
    public static void setRoles(List<String> roles) { HOLDER.get().roles = roles; }
    public static List<String> getRoles() { return HOLDER.get().roles != null ? HOLDER.get().roles : List.of(); }
    public static void setPerms(List<String> perms) { HOLDER.get().perms = perms; }
    public static List<String> getPerms() { return HOLDER.get().perms != null ? HOLDER.get().perms : List.of(); }

    public static void clear() { HOLDER.remove(); }

    private static class Holder {
        UUID tenantId;
        String correlationId;
        String subject;
        String plan;
        String region;
        List<String> roles;
        List<String> perms;
    }
}
