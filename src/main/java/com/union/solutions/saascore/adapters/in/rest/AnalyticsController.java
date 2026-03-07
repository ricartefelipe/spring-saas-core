package com.union.solutions.saascore.adapters.in.rest;

import com.union.solutions.saascore.application.abac.AbacContext;
import com.union.solutions.saascore.application.abac.AbacEvaluator;
import com.union.solutions.saascore.application.abac.AbacResult;
import com.union.solutions.saascore.application.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/analytics")
public class AnalyticsController {

  private final AnalyticsService analyticsService;
  private final AbacEvaluator abacEvaluator;

  public AnalyticsController(AnalyticsService analyticsService, AbacEvaluator abacEvaluator) {
    this.analyticsService = analyticsService;
    this.abacEvaluator = abacEvaluator;
  }

  @GetMapping("/summary")
  public ResponseEntity<?> summary() {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("analytics:read"));
    if (!abac.allowed())
      return ResponseEntity.status(403)
          .body(ProblemDetails.of(
              403, "Forbidden", abac.reason(), "/v1/analytics/summary", null));

    return ResponseEntity.ok(analyticsService.getSummary());
  }

  @GetMapping("/anomalies")
  public ResponseEntity<?> anomalies() {
    AbacResult abac = abacEvaluator.evaluate(AbacContext.fromCurrentContext("analytics:read"));
    if (!abac.allowed())
      return ResponseEntity.status(403)
          .body(ProblemDetails.of(
              403, "Forbidden", abac.reason(), "/v1/analytics/anomalies", null));

    return ResponseEntity.ok(analyticsService.detectAnomalies());
  }
}
