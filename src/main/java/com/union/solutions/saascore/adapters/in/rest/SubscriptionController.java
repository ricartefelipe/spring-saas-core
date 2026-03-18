package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.application.billing.SubscriptionUseCase;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.Subscription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/subscriptions")
public class SubscriptionController {

  private final SubscriptionUseCase subscriptionUseCase;

  public SubscriptionController(SubscriptionUseCase subscriptionUseCase) {
    this.subscriptionUseCase = subscriptionUseCase;
  }

  @PostMapping("/trial")
  public ResponseEntity<?> startTrial(@Valid @RequestBody PlanRequest request) {
    UUID tenantId = requireTenantId();
    try {
      Subscription sub = subscriptionUseCase.startTrial(tenantId, request.planSlug());
      return ResponseEntity.status(201).body(SubscriptionResponse.from(sub));
    } catch (IllegalStateException | IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .body(
              ProblemDetails.of(
                  400, "Bad Request", e.getMessage(), "/v1/subscriptions/trial", null));
    }
  }

  @PostMapping("/activate")
  public ResponseEntity<?> activate() {
    UUID tenantId = requireTenantId();
    try {
      Subscription sub = subscriptionUseCase.activate(tenantId);
      return ResponseEntity.ok(SubscriptionResponse.from(sub));
    } catch (IllegalStateException e) {
      return ResponseEntity.badRequest()
          .body(
              ProblemDetails.of(
                  400, "Bad Request", e.getMessage(), "/v1/subscriptions/activate", null));
    }
  }

  @PostMapping("/upgrade")
  public ResponseEntity<?> upgrade(@Valid @RequestBody PlanRequest request) {
    UUID tenantId = requireTenantId();
    try {
      Subscription sub = subscriptionUseCase.upgrade(tenantId, request.planSlug());
      return ResponseEntity.ok(SubscriptionResponse.from(sub));
    } catch (IllegalStateException | IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .body(
              ProblemDetails.of(
                  400, "Bad Request", e.getMessage(), "/v1/subscriptions/upgrade", null));
    }
  }

  @PostMapping("/downgrade")
  public ResponseEntity<?> downgrade(@Valid @RequestBody PlanRequest request) {
    UUID tenantId = requireTenantId();
    try {
      Subscription sub = subscriptionUseCase.downgrade(tenantId, request.planSlug());
      return ResponseEntity.ok(SubscriptionResponse.from(sub));
    } catch (IllegalStateException | IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .body(
              ProblemDetails.of(
                  400, "Bad Request", e.getMessage(), "/v1/subscriptions/downgrade", null));
    }
  }

  @PostMapping("/cancel")
  public ResponseEntity<?> cancel() {
    UUID tenantId = requireTenantId();
    try {
      Subscription sub = subscriptionUseCase.cancel(tenantId);
      return ResponseEntity.ok(SubscriptionResponse.from(sub));
    } catch (IllegalStateException e) {
      return ResponseEntity.badRequest()
          .body(
              ProblemDetails.of(
                  400, "Bad Request", e.getMessage(), "/v1/subscriptions/cancel", null));
    }
  }

  @PostMapping("/schedule-cancel")
  public ResponseEntity<?> scheduleCancelAtPeriodEnd() {
    UUID tenantId = requireTenantId();
    try {
      Subscription sub = subscriptionUseCase.scheduleCancelAtPeriodEnd(tenantId);
      return ResponseEntity.ok(SubscriptionResponse.from(sub));
    } catch (IllegalStateException e) {
      return ResponseEntity.badRequest()
          .body(
              ProblemDetails.of(
                  400, "Bad Request", e.getMessage(), "/v1/subscriptions/schedule-cancel", null));
    }
  }

  @PostMapping("/undo-schedule-cancel")
  public ResponseEntity<?> undoScheduleCancelAtPeriodEnd() {
    UUID tenantId = requireTenantId();
    try {
      Subscription sub = subscriptionUseCase.undoScheduleCancelAtPeriodEnd(tenantId);
      return ResponseEntity.ok(SubscriptionResponse.from(sub));
    } catch (IllegalStateException e) {
      return ResponseEntity.badRequest()
          .body(
              ProblemDetails.of(
                  400, "Bad Request", e.getMessage(), "/v1/subscriptions/undo-schedule-cancel", null));
    }
  }

  @PostMapping("/reactivate")
  public ResponseEntity<?> reactivate() {
    UUID tenantId = requireTenantId();
    try {
      Subscription sub = subscriptionUseCase.reactivate(tenantId);
      return ResponseEntity.ok(SubscriptionResponse.from(sub));
    } catch (IllegalStateException e) {
      return ResponseEntity.badRequest()
          .body(
              ProblemDetails.of(
                  400, "Bad Request", e.getMessage(), "/v1/subscriptions/reactivate", null));
    }
  }

  @GetMapping("/current")
  public ResponseEntity<?> getCurrentSubscription() {
    UUID tenantId = requireTenantId();
    try {
      Subscription sub = subscriptionUseCase.getCurrentSubscription(tenantId);
      return ResponseEntity.ok(SubscriptionResponse.from(sub));
    } catch (IllegalStateException e) {
      return ResponseEntity.notFound().build();
    }
  }

  private UUID requireTenantId() {
    return TenantContext.getTenantId()
        .orElseThrow(() -> new IllegalStateException("Tenant context not available"));
  }

  public record PlanRequest(@NotBlank String planSlug) {}

  public record SubscriptionResponse(
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
      boolean cancelAtPeriodEnd,
      Instant createdAt,
      Instant updatedAt) {

    public static SubscriptionResponse from(Subscription s) {
      return new SubscriptionResponse(
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
          s.isCancelAtPeriodEnd(),
          s.getCreatedAt(),
          s.getUpdatedAt());
    }
  }
}
