package com.yourorg.saascore.config;

import java.util.Optional;
import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<Holder> HOLDER = ThreadLocal.withInitial(Holder::new);

    private TenantContext() {}

    public static void setTenantId(UUID tenantId) {
        HOLDER.get().tenantId = tenantId;
    }

    public static Optional<UUID> getTenantId() {
        return Optional.ofNullable(HOLDER.get().tenantId);
    }

    public static void setRegion(String region) {
        HOLDER.get().region = region;
    }

    public static String getRegion() {
        String r = HOLDER.get().region;
        return r != null ? r : "region-a";
    }

    public static void setConsistency(String consistency) {
        HOLDER.get().consistency = consistency;
    }

    public static String getConsistency() {
        String c = HOLDER.get().consistency;
        return c != null ? c : "strong";
    }

    public static void setCorrelationId(String correlationId) {
        HOLDER.get().correlationId = correlationId;
    }

    public static String getCorrelationId() {
        return HOLDER.get().correlationId;
    }

    public static void setSubject(String subject) {
        HOLDER.get().subject = subject;
    }

    public static String getSubject() {
        return HOLDER.get().subject;
    }

    public static void setShardKey(String shardKey) {
        HOLDER.get().shardKey = shardKey;
    }

    public static String getShardKey() {
        return HOLDER.get().shardKey;
    }

    public static void setPrimaryRegion(String primaryRegion) {
        HOLDER.get().primaryRegion = primaryRegion;
    }

    public static String getPrimaryRegion() {
        return HOLDER.get().primaryRegion;
    }

    public static void setPlan(String plan) {
        HOLDER.get().plan = plan;
    }

    public static String getPlan() {
        String p = HOLDER.get().plan;
        return p != null ? p : "free";
    }

    public static void setJti(String jti) {
        HOLDER.get().jti = jti;
    }

    public static String getJti() {
        return HOLDER.get().jti;
    }

    public static void clear() {
        HOLDER.remove();
    }

    private static class Holder {
        UUID tenantId;
        String region;
        String consistency;
        String correlationId;
        String subject;
        String shardKey;
        String primaryRegion;
        String plan;
        String jti;
    }
}
