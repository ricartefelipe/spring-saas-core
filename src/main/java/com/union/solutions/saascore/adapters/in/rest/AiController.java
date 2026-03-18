package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.application.abac.AbacContext;
import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.abac.AbacResult;
import com.union.solutions.saascore.application.ai.GovernanceChatbotService;
import com.union.solutions.saascore.application.ai.GovernanceRecommendationService;
import com.union.solutions.saascore.application.service.AiDocsService;
import com.union.solutions.saascore.application.service.AiService;
import com.union.solutions.saascore.config.AiConfig.AiProperties;
import com.union.solutions.saascore.config.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/ai")
@Tag(
    name = "AI/LLM",
    description = "Intelligent governance analysis powered by LLM with rule-engine fallback")
public class AiController {

  private final AiService aiService;
  private final AiDocsService aiDocsService;
  private final AbacEvaluator abacEvaluator;
  private final AiProperties aiProperties;
  private final GovernanceChatbotService chatbotService;
  private final GovernanceRecommendationService governanceRecommendationService;

  public AiController(
      AiService aiService,
      AiDocsService aiDocsService,
      AbacEvaluator abacEvaluator,
      AiProperties aiProperties,
      GovernanceChatbotService chatbotService,
      GovernanceRecommendationService governanceRecommendationService) {
    this.aiService = aiService;
    this.aiDocsService = aiDocsService;
    this.abacEvaluator = abacEvaluator;
    this.aiProperties = aiProperties;
    this.chatbotService = chatbotService;
    this.governanceRecommendationService = governanceRecommendationService;
  }

  @GetMapping("/status")
  @Operation(summary = "AI engine status and capabilities")
  public ResponseEntity<Map<String, Object>> status() {
    return ResponseEntity.ok(
        Map.of(
            "engine", aiProperties.isEnabled() ? "llm" : "rule-engine",
            "provider", aiProperties.isEnabled() ? aiProperties.getProvider() : "built-in",
            "model", aiProperties.isEnabled() ? aiProperties.getModel() : "rule-based-v1",
            "capabilities",
                java.util.List.of(
                    "audit-analysis",
                    "governance-recommendations",
                    "chat-assistant",
                    "system-insights",
                    "anomaly-detection")));
  }

  @PostMapping("/analyze-audit")
  @Operation(summary = "AI-powered audit log analysis")
  public ResponseEntity<?> analyzeAudit(
      @RequestParam(required = false) String tenantId,
      @RequestParam(defaultValue = "24") @Min(1) @Max(720) int hoursBack) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("analytics:read"));
    if (!abac.allowed()) {
      return forbidden(abac, "/v1/ai/analyze-audit");
    }
    return ResponseEntity.ok(aiService.analyzeAudit(tenantId, hoursBack));
  }

  @PostMapping("/recommendations")
  @Operation(summary = "AI governance recommendations for a tenant")
  public ResponseEntity<?> recommendations(@RequestParam(required = false) String tenantId) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("analytics:read"));
    if (!abac.allowed()) {
      return forbidden(abac, "/v1/ai/recommendations");
    }
    return ResponseEntity.ok(aiService.getRecommendations(tenantId));
  }

  @PostMapping("/chat")
  @Operation(summary = "Conversational AI assistant for platform governance")
  public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("analytics:read"));
    if (!abac.allowed()) {
      return forbidden(abac, "/v1/ai/chat");
    }
    UUID tenantId = resolveTenantId(request.tenantId());
    String question = request.question() != null ? request.question() : request.message();
    GovernanceChatbotService.ChatResponse chatResponse = chatbotService.chat(tenantId, question);
    return ResponseEntity.ok(chatResponse);
  }

  @GetMapping("/insights")
  @Operation(summary = "Fluxe B2B Suite insights and health indicators")
  public ResponseEntity<?> insights() {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("analytics:read"));
    if (!abac.allowed()) {
      return forbidden(abac, "/v1/ai/insights");
    }
    return ResponseEntity.ok(aiService.getInsights());
  }

  @GetMapping("/docs")
  @Operation(
      summary = "Live API documentation summary",
      description =
          "Structured JSON summary of tenants, policies, flags, audit and API surface for LLM consumption")
  public ResponseEntity<?> liveDocs() {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("analytics:read"));
    if (!abac.allowed()) {
      return forbidden(abac, "/v1/ai/docs");
    }
    return ResponseEntity.ok(aiDocsService.buildLiveDocs());
  }

  @GetMapping("/docs/tenant/{id}")
  @Operation(
      summary = "Tenant-specific live documentation",
      description =
          "Tenant details, applicable policies, feature flags and recent audit activity for LLM consumption")
  public ResponseEntity<?> tenantDocs(@PathVariable UUID id) {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("analytics:read"));
    if (!abac.allowed()) {
      return forbidden(abac, "/v1/ai/docs/tenant/" + id);
    }
    return aiDocsService
        .buildTenantDocs(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/recommendations")
  @Operation(summary = "Governance recommendations for a tenant based on current configuration")
  public ResponseEntity<?> governanceRecommendations() {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("analytics:read"));
    if (!abac.allowed()) {
      return forbidden(abac, "/v1/ai/recommendations");
    }
    UUID tenantId = resolveTenantId(null);
    if (tenantId == null) {
      return ResponseEntity.badRequest()
          .body(
              ProblemDetails.of(
                  400,
                  "Bad Request",
                  "Tenant ID required. Authenticate with a JWT containing `tid`.",
                  "/v1/ai/recommendations",
                  null));
    }
    return ResponseEntity.ok(governanceRecommendationService.analyzeGovernance(tenantId));
  }

  private UUID resolveTenantId(String requestTenantId) {
    if (requestTenantId != null && !requestTenantId.isBlank()) {
      try {
        return UUID.fromString(requestTenantId);
      } catch (IllegalArgumentException e) {
        return TenantContext.getTenantId().orElse(null);
      }
    }
    return TenantContext.getTenantId().orElse(null);
  }

  private ResponseEntity<ProblemDetails> forbidden(AbacResult abac, String instance) {
    return ResponseEntity.status(403)
        .body(ProblemDetails.of(403, "Forbidden", abac.reason(), instance, null));
  }

  public record ChatRequest(String message, String question, String tenantId) {
    public ChatRequest {
      if ((message == null || message.isBlank()) && (question == null || question.isBlank())) {
        throw new IllegalArgumentException("Either 'message' or 'question' must be provided");
      }
    }
  }
}
