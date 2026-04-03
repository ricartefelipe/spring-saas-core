package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.billing.BillingUseCase;
import com.union.solutions.saascore.application.port.StripeBillingPort;
import com.union.solutions.saascore.application.port.TenantRepository;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.PlanDefinition;
import com.union.solutions.saascore.domain.Subscription;
import com.union.solutions.saascore.domain.Tenant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/billing")
public class BillingController {

  private final BillingUseCase billingUseCase;
  private final StripeBillingPort billingPort;
  private final TenantRepository tenantRepo;
  private final AbacEvaluator abacEvaluator;

  public BillingController(
      BillingUseCase billingUseCase,
      StripeBillingPort billingPort,
      TenantRepository tenantRepo,
      AbacEvaluator abacEvaluator) {
    this.billingUseCase = billingUseCase;
    this.billingPort = billingPort;
    this.tenantRepo = tenantRepo;
    this.abacEvaluator = abacEvaluator;
  }

  @GetMapping("/plans")
  public ResponseEntity<List<PlanDto>> listPlans() {
    List<PlanDto> plans = billingUseCase.listPlans().stream().map(PlanDto::from).toList();
    return ResponseEntity.ok(plans);
  }

  @GetMapping("/plans/{slug}")
  public ResponseEntity<?> getPlan(@PathVariable String slug) {
    return billingUseCase
        .getPlan(slug)
        .map(p -> ResponseEntity.ok(PlanDto.from(p)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/subscriptions")
  public ResponseEntity<?> createSubscription(
      @Valid @RequestBody CreateSubscriptionRequest request) {
    abacEvaluator.enforceOrThrow("billing:write");
    UUID tenantId =
        TenantContext.getTenantId()
            .orElseThrow(() -> new IllegalStateException("Tenant context not available"));
    try {
      Subscription sub = billingUseCase.createSubscription(tenantId, request.planSlug());
      return ResponseEntity.status(201).body(SubscriptionDto.from(sub));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .body(
              ProblemDetails.of(
                  400, "Bad Request", e.getMessage(), "/v1/billing/subscriptions", null));
    }
  }

  @GetMapping("/subscriptions/current")
  public ResponseEntity<?> getCurrentSubscription() {
    abacEvaluator.enforceOrThrow("profile:read");
    UUID tenantId =
        TenantContext.getTenantId()
            .orElseThrow(() -> new IllegalStateException("Tenant context not available"));
    return billingUseCase
        .getSubscription(tenantId)
        .map(s -> ResponseEntity.ok(SubscriptionDto.from(s)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/subscriptions/cancel")
  public ResponseEntity<?> cancelSubscription() {
    abacEvaluator.enforceOrThrow("billing:write");
    UUID tenantId =
        TenantContext.getTenantId()
            .orElseThrow(() -> new IllegalStateException("Tenant context not available"));
    return billingUseCase
        .cancelSubscription(tenantId)
        .map(s -> ResponseEntity.ok(SubscriptionDto.from(s)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/portal-session")
  public ResponseEntity<?> createPortalSession(@RequestBody Map<String, String> req) {
    abacEvaluator.enforceOrThrow("billing:write");
    UUID tenantId =
        TenantContext.getTenantId()
            .orElseThrow(() -> new IllegalStateException("Tenant context not available"));

    Tenant tenant =
        tenantRepo
            .findById(tenantId)
            .orElseThrow(() -> new IllegalStateException("Tenant not found"));

    if (tenant.getStripeCustomerId() == null || tenant.getStripeCustomerId().isBlank()) {
      return ResponseEntity.badRequest()
          .body(
              ProblemDetails.of(
                  400,
                  "Bad Request",
                  "Tenant has no Stripe customer configured",
                  "/v1/billing/portal-session",
                  null));
    }

    String returnUrl = req.getOrDefault("returnUrl", "/billing");
    String url = billingPort.createBillingPortalSession(tenant.getStripeCustomerId(), returnUrl);
    return ResponseEntity.ok(Map.of("url", url));
  }

  public record CreateSubscriptionRequest(@NotBlank String planSlug) {}

  public record PlanDto(
      UUID id,
      String slug,
      String displayName,
      String description,
      long monthlyPriceCents,
      long yearlyPriceCents,
      int maxUsers,
      int maxProjects,
      int storageGb,
      boolean active,
      Instant createdAt,
      Instant updatedAt) {

    public static PlanDto from(PlanDefinition p) {
      return new PlanDto(
          p.getId(),
          p.getSlug(),
          p.getDisplayName(),
          p.getDescription(),
          p.getMonthlyPriceCents(),
          p.getYearlyPriceCents(),
          p.getMaxUsers(),
          p.getMaxProjects(),
          p.getStorageGb(),
          p.isActive(),
          p.getCreatedAt(),
          p.getUpdatedAt());
    }
  }

  public record SubscriptionDto(
      UUID id,
      UUID tenantId,
      String planSlug,
      String status,
      Instant currentPeriodStart,
      Instant currentPeriodEnd,
      Instant trialEndsAt,
      Instant gracePeriodEndsAt,
      String previousPlanSlug,
      Instant cancelledAt,
      Instant createdAt,
      Instant updatedAt) {

    public static SubscriptionDto from(Subscription s) {
      return new SubscriptionDto(
          s.getId(),
          s.getTenantId(),
          s.getPlanSlug(),
          s.getStatus().name(),
          s.getCurrentPeriodStart(),
          s.getCurrentPeriodEnd(),
          s.getTrialEndsAt(),
          s.getGracePeriodEndsAt(),
          s.getPreviousPlanSlug(),
          s.getCancelledAt(),
          s.getCreatedAt(),
          s.getUpdatedAt());
    }
  }
}
