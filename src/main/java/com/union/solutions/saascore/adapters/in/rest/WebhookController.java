package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.application.abac.AbacContext;
import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.abac.AbacResult;
import com.union.solutions.saascore.application.webhook.WebhookUseCase;
import com.union.solutions.saascore.config.TenantContext;
import com.union.solutions.saascore.domain.WebhookEndpoint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/webhooks")
public class WebhookController {

  private final WebhookUseCase webhookUseCase;
  private final AbacEvaluator abacEvaluator;

  public WebhookController(WebhookUseCase webhookUseCase, AbacEvaluator abacEvaluator) {
    this.webhookUseCase = webhookUseCase;
    this.abacEvaluator = abacEvaluator;
  }

  @PostMapping
  public ResponseEntity<?> register(@Valid @RequestBody RegisterWebhookRequest request) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("webhooks:manage"));
    if (!abac.allowed())
      return ResponseEntity.status(403)
          .body(ProblemDetails.of(403, "Forbidden", abac.reason(), "/v1/webhooks", null));
    UUID tenantId =
        TenantContext.getTenantId()
            .orElseThrow(() -> new IllegalStateException("Tenant context required"));
    WebhookEndpoint endpoint =
        webhookUseCase.register(tenantId, request.url(), request.secret(), request.events());
    return ResponseEntity.status(201).body(WebhookDto.from(endpoint));
  }

  @GetMapping
  public ResponseEntity<?> list() {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("webhooks:manage"));
    if (!abac.allowed())
      return ResponseEntity.status(403)
          .body(ProblemDetails.of(403, "Forbidden", abac.reason(), "/v1/webhooks", null));
    UUID tenantId =
        TenantContext.getTenantId()
            .orElseThrow(() -> new IllegalStateException("Tenant context required"));
    List<WebhookDto> items =
        webhookUseCase.listByTenant(tenantId).stream().map(WebhookDto::from).toList();
    return ResponseEntity.ok(items);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("webhooks:manage"));
    if (!abac.allowed()) return ResponseEntity.status(403).build();
    UUID tenantId =
        TenantContext.getTenantId()
            .orElseThrow(() -> new IllegalStateException("Tenant context required"));
    return webhookUseCase.delete(id, tenantId)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }

  public record RegisterWebhookRequest(
      @NotBlank String url, @NotBlank String secret, @NotNull List<String> events) {}

  public record WebhookDto(
      UUID id,
      UUID tenantId,
      String url,
      List<String> events,
      boolean active,
      java.time.Instant createdAt,
      java.time.Instant updatedAt) {
    public static WebhookDto from(WebhookEndpoint w) {
      return new WebhookDto(
          w.getId(),
          w.getTenantId(),
          w.getUrl(),
          w.getEvents() != null ? w.getEvents() : List.of(),
          w.isActive(),
          w.getCreatedAt(),
          w.getUpdatedAt());
    }
  }
}
