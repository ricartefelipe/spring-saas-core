package com.union.solutions.saascore.domain;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class Policy {

  /** Alinhado a node-b2b-orders (AbacGuard) e py-payments-ledger (_PLAN_TIER). */
  private static final Map<String, Integer> PLAN_TIER =
      Map.of("free", 0, "starter", 1, "pro", 2, "enterprise", 3);

  private UUID id;
  private String permissionCode;
  private Effect effect;
  private List<String> allowedPlans;
  private List<String> allowedRegions;
  private boolean enabled;
  private String notes;
  private Instant createdAt;
  private Instant updatedAt;

  public Policy() {}

  public Policy(
      UUID id,
      String permissionCode,
      Effect effect,
      List<String> allowedPlans,
      List<String> allowedRegions,
      boolean enabled,
      String notes,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.permissionCode = permissionCode;
    this.effect = effect;
    this.allowedPlans = allowedPlans != null ? allowedPlans : List.of();
    this.allowedRegions = allowedRegions != null ? allowedRegions : List.of();
    this.enabled = enabled;
    this.notes = notes;
    this.createdAt = createdAt != null ? createdAt : Instant.now();
    this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
  }

  public boolean appliesTo(String plan, String region) {
    if (!enabled) return false;
    String normalizedPlan = normalizePlan(plan);
    boolean planMatch =
        allowedPlans.isEmpty()
            || (effect == Effect.ALLOW
                ? planMatchesTierAllow(normalizedPlan)
                : planMatchesExact(normalizedPlan));
    boolean regionMatch = allowedRegions.isEmpty() || allowedRegions.contains(region);
    return planMatch && regionMatch;
  }

  private static String normalizePlan(String plan) {
    if (plan == null || plan.isBlank()) {
      return "free";
    }
    return plan.toLowerCase(Locale.ROOT).trim();
  }

  /** DENY: plano deve coincidir com um slug em allowedPlans (sem upgrade por tier). */
  private boolean planMatchesExact(String normalizedPlan) {
    for (String ap : allowedPlans) {
      if (ap != null && normalizePlan(ap).equals(normalizedPlan)) {
        return true;
      }
    }
    return false;
  }

  /**
   * ALLOW: min(tier dos slugs conhecidos); tier(tenant) &gt;= esse mínimo (ex.: só "pro" permite
   * "enterprise"), alinhado a node-b2b-orders / py-payments-ledger.
   */
  private boolean planMatchesTierAllow(String normalizedPlan) {
    for (String ap : allowedPlans) {
      if (ap != null && normalizePlan(ap).equals(normalizedPlan)) {
        return true;
      }
    }
    Integer userTier = PLAN_TIER.get(normalizedPlan);
    if (userTier == null) {
      return false;
    }
    int minTier = Integer.MAX_VALUE;
    boolean anyKnown = false;
    for (String ap : allowedPlans) {
      if (ap == null) {
        continue;
      }
      Integer t = PLAN_TIER.get(normalizePlan(ap));
      if (t != null) {
        anyKnown = true;
        minTier = Math.min(minTier, t);
      }
    }
    if (!anyKnown) {
      return false;
    }
    return userTier >= minTier;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getPermissionCode() {
    return permissionCode;
  }

  public void setPermissionCode(String permissionCode) {
    this.permissionCode = permissionCode;
  }

  public Effect getEffect() {
    return effect;
  }

  public void setEffect(Effect effect) {
    this.effect = effect;
  }

  public List<String> getAllowedPlans() {
    return allowedPlans;
  }

  public void setAllowedPlans(List<String> allowedPlans) {
    this.allowedPlans = allowedPlans;
  }

  public List<String> getAllowedRegions() {
    return allowedRegions;
  }

  public void setAllowedRegions(List<String> allowedRegions) {
    this.allowedRegions = allowedRegions;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Policy that)) return false;
    return java.util.Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(id);
  }

  public enum Effect {
    ALLOW,
    DENY
  }
}
