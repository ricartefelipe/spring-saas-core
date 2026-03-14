package com.union.solutions.saascore.application.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.union.solutions.saascore.adapters.out.persistence.AuditLogJpaRepository;
import com.union.solutions.saascore.adapters.out.persistence.TenantJpaRepository;
import com.union.solutions.saascore.config.AiConfig.AiProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AiService {

  private static final Logger log = LoggerFactory.getLogger(AiService.class);
  private static final String SYSTEM_PROMPT =
      """
      You are the AI governance assistant for Fluxe B2B Suite, a multi-tenant SaaS platform.
      You analyze audit logs, tenant behavior, security policies, and operational metrics.
      Always respond in the user's language (default: pt-BR).
      Be concise, actionable, and security-focused.
      Format responses with clear sections using markdown.
      When analyzing data, identify: anomalies, security risks, optimization opportunities,
      and compliance gaps. Prioritize findings by severity (critical > high > medium > low).
      """;

  private final RestClient aiRestClient;
  private final AiProperties aiProperties;
  private final AuditLogJpaRepository auditRepo;
  private final TenantJpaRepository tenantRepo;
  private final AnalyticsService analyticsService;
  private final Counter aiRequestCounter;
  private final Counter aiFallbackCounter;
  private final Timer aiLatencyTimer;

  public AiService(
      @Qualifier("aiRestClient") RestClient aiRestClient,
      AiProperties aiProperties,
      AuditLogJpaRepository auditRepo,
      TenantJpaRepository tenantRepo,
      AnalyticsService analyticsService,
      MeterRegistry meterRegistry) {
    this.aiRestClient = aiRestClient;
    this.aiProperties = aiProperties;
    this.auditRepo = auditRepo;
    this.tenantRepo = tenantRepo;
    this.analyticsService = analyticsService;
    this.aiRequestCounter =
        Counter.builder("ai.requests.total")
            .description("Total AI requests")
            .register(meterRegistry);
    this.aiFallbackCounter =
        Counter.builder("ai.fallback.total")
            .description("Times AI fell back to rule-based")
            .register(meterRegistry);
    this.aiLatencyTimer =
        Timer.builder("ai.request.duration")
            .description("AI request latency")
            .register(meterRegistry);
  }

  @CircuitBreaker(name = "aiService", fallbackMethod = "analyzeAuditFallback")
  public AiResponse analyzeAudit(String tenantId, int hoursBack) {
    aiRequestCounter.increment();
    return aiLatencyTimer.record(
        () -> {
          var auditData = gatherAuditContext(tenantId, hoursBack);
          String userPrompt =
              String.format(
                  """
          Analyze the following audit log data for tenant '%s' (last %d hours):

          Total events: %s
          Actions breakdown: %s
          Access denied events: %s
          Unique users: %s
          Anomalies detected (rule-based): %s

          Provide:
          1. Security assessment (are there suspicious patterns?)
          2. Top 3 risks identified
          3. Recommended actions (specific, implementable)
          4. Compliance status (LGPD/GDPR considerations)
          """,
                  tenantId,
                  hoursBack,
                  auditData.get("totalEvents"),
                  auditData.get("actionBreakdown"),
                  auditData.get("accessDenied"),
                  auditData.get("uniqueUsers"),
                  auditData.get("anomalies"));

          if (!aiProperties.isEnabled()) {
            return buildRuleBasedAuditAnalysis(auditData, tenantId);
          }

          String llmResponse = callLlm(SYSTEM_PROMPT, userPrompt);
          return new AiResponse("llm", llmResponse, auditData);
        });
  }

  @CircuitBreaker(name = "aiService", fallbackMethod = "getRecommendationsFallback")
  public AiResponse getRecommendations(String tenantId) {
    aiRequestCounter.increment();
    return aiLatencyTimer.record(
        () -> {
          var context = gatherGovernanceContext(tenantId);
          String userPrompt =
              String.format(
                  """
          Based on this tenant governance data:

          Tenant: %s (plan: %s, status: %s, region: %s)
          Active policies: %s
          Feature flags: %s enabled / %s total
          Audit events (30d): %s
          Access denied (30d): %s

          Generate governance recommendations:
          1. Policy optimization (are there redundant or missing policies?)
          2. Feature flag hygiene (stale flags, rollout suggestions)
          3. Security hardening (based on plan and usage patterns)
          4. Cost optimization (based on plan utilization)
          5. Compliance checklist items for this tenant
          """,
                  context.get("tenantName"),
                  context.get("plan"),
                  context.get("status"),
                  context.get("region"),
                  context.get("policyCount"),
                  context.get("enabledFlags"),
                  context.get("totalFlags"),
                  context.get("auditCount"),
                  context.get("deniedCount"));

          if (!aiProperties.isEnabled()) {
            return buildRuleBasedRecommendations(context, tenantId);
          }

          String llmResponse = callLlm(SYSTEM_PROMPT, userPrompt);
          return new AiResponse("llm", llmResponse, context);
        });
  }

  @CircuitBreaker(name = "aiService", fallbackMethod = "chatFallback")
  public AiResponse chat(String message, String tenantId) {
    aiRequestCounter.increment();
    return aiLatencyTimer.record(
        () -> {
          var summary = analyticsService.getSummary();
          long activeTenants = summary.tenants().byStatus().getOrDefault("ACTIVE", 0L);
          String contextPrompt =
              String.format(
                  """
          Current system state:
          - Total tenants: %d (active: %d)
          - Total policies: %d
          - Audit events (24h): %d
          - User is asking about tenant: %s

          User question: %s
          """,
                  summary.tenants().total(),
                  activeTenants,
                  summary.policies().total(),
                  summary.audit().last24h(),
                  tenantId != null ? tenantId : "global",
                  message);

          if (!aiProperties.isEnabled()) {
            return buildRuleBasedChatResponse(message, summary);
          }

          String llmResponse = callLlm(SYSTEM_PROMPT, contextPrompt);
          return new AiResponse("llm", llmResponse, Map.of("question", message));
        });
  }

  public AiResponse getInsights() {
    aiRequestCounter.increment();
    var summary = analyticsService.getSummary();
    var anomalies = analyticsService.detectAnomalies();

    List<Insight> insights = new ArrayList<>();

    long activeTenants = summary.tenants().byStatus().getOrDefault("ACTIVE", 0L);

    if (activeTenants == 0) {
      insights.add(
          new Insight(
              "critical",
              "Nenhum tenant ativo",
              "O sistema não possui tenants ativos. Configure ao menos um tenant para operação."));
    }

    if (summary.policies().total() == 0) {
      insights.add(
          new Insight(
              "high",
              "Sem políticas de governança",
              "Nenhuma política ABAC configurada. O sistema opera apenas com RBAC básico."));
    }

    long inactiveTenants = summary.tenants().total() - activeTenants;
    if (inactiveTenants > activeTenants && summary.tenants().total() > 2) {
      insights.add(
          new Insight(
              "medium",
              "Alto índice de tenants inativos",
              String.format(
                  "%d de %d tenants estão inativos (%.0f%%). Considere uma campanha de reativação.",
                  inactiveTenants,
                  summary.tenants().total(),
                  (double) inactiveTenants / summary.tenants().total() * 100)));
    }

    if (!anomalies.anomalies().isEmpty()) {
      insights.add(
          new Insight(
              "high",
              "Anomalias detectadas",
              String.format(
                  "%d anomalias identificadas: %s",
                  anomalies.anomalies().size(),
                  anomalies.anomalies().stream()
                      .map(AnalyticsService.Anomaly::type)
                      .distinct()
                      .reduce((a, b) -> a + ", " + b)
                      .orElse("N/A"))));
    }

    if (summary.flags().enabled() > 20) {
      insights.add(
          new Insight(
              "medium",
              "Feature flags em excesso",
              String.format(
                  "%d flags ativas. Revise flags antigas que podem ser promovidas a permanentes.",
                  summary.flags().enabled())));
    }

    if (insights.isEmpty()) {
      insights.add(
          new Insight(
              "info",
              "Sistema saudável",
              "Nenhuma anomalia ou problema de governança detectado no momento."));
    }

    return new AiResponse(
        "rule-engine", null, Map.of("insights", insights, "generatedAt", Instant.now().toString()));
  }

  private String callLlm(String systemPrompt, String userPrompt) {
    var requestBody =
        Map.of(
            "model", aiProperties.getModel(),
            "messages",
                List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)),
            "max_tokens", aiProperties.getMaxTokens(),
            "temperature", aiProperties.getTemperature());

    JsonNode response =
        aiRestClient
            .post()
            .uri("/chat/completions")
            .body(requestBody)
            .retrieve()
            .body(JsonNode.class);

    if (response != null && response.has("choices") && !response.get("choices").isEmpty()) {
      return response.get("choices").get(0).get("message").get("content").asText();
    }
    throw new com.union.solutions.saascore.domain.exception.AiServiceException("Empty LLM response");
  }

  @SuppressWarnings("unused")
  private AiResponse analyzeAuditFallback(String tenantId, int hoursBack, Throwable t) {
    log.warn("AI audit analysis fallback triggered: {}", t.getMessage());
    aiFallbackCounter.increment();
    var auditData = gatherAuditContext(tenantId, hoursBack);
    return buildRuleBasedAuditAnalysis(auditData, tenantId);
  }

  @SuppressWarnings("unused")
  private AiResponse getRecommendationsFallback(String tenantId, Throwable t) {
    log.warn("AI recommendations fallback triggered: {}", t.getMessage());
    aiFallbackCounter.increment();
    var context = gatherGovernanceContext(tenantId);
    return buildRuleBasedRecommendations(context, tenantId);
  }

  @SuppressWarnings("unused")
  private AiResponse chatFallback(String message, String tenantId, Throwable t) {
    log.warn("AI chat fallback triggered: {}", t.getMessage());
    aiFallbackCounter.increment();
    var summary = analyticsService.getSummary();
    return buildRuleBasedChatResponse(message, summary);
  }

  private Map<String, Object> gatherAuditContext(String tenantId, int hoursBack) {
    Timestamp since = Timestamp.from(Instant.now().minus(hoursBack, ChronoUnit.HOURS));
    Map<String, Object> data = new HashMap<>();
    data.put("totalEvents", auditRepo.count());
    data.put("accessDenied", 0);
    data.put("uniqueUsers", 0);
    data.put("actionBreakdown", "N/A");
    data.put("anomalies", analyticsService.detectAnomalies().anomalies().size());
    return data;
  }

  private Map<String, Object> gatherGovernanceContext(String tenantId) {
    Map<String, Object> ctx = new HashMap<>();
    var summary = analyticsService.getSummary();
    ctx.put("tenantName", tenantId != null ? tenantId : "global");
    ctx.put("plan", "N/A");
    ctx.put("status", "N/A");
    ctx.put("region", "N/A");
    ctx.put("policyCount", summary.policies().total());
    ctx.put("enabledFlags", summary.flags().enabled());
    ctx.put("totalFlags", summary.flags().total());
    ctx.put("auditCount", summary.audit().last7d());
    ctx.put("deniedCount", 0);
    return ctx;
  }

  private AiResponse buildRuleBasedAuditAnalysis(Map<String, Object> data, String tenantId) {
    int anomalyCount = ((Number) data.getOrDefault("anomalies", 0)).intValue();
    String severity = anomalyCount > 5 ? "CRITICAL" : anomalyCount > 0 ? "WARNING" : "OK";

    String analysis =
        String.format(
            """
        ## Análise de Auditoria — %s
        **Motor**: Rule Engine (configure OPENAI_API_KEY para análise com IA)

        ### Status de Segurança: %s
        - Eventos totais: %s
        - Anomalias detectadas: %d

        ### Recomendações
        1. %s
        2. Revise permissões de acesso periodicamente
        3. Configure alertas para eventos ACCESS_DENIED frequentes
        4. Mantenha retenção de logs conforme política de compliance
        """,
            tenantId != null ? tenantId : "global",
            severity,
            data.get("totalEvents"),
            anomalyCount,
            anomalyCount > 0
                ? "**AÇÃO URGENTE**: Investigue as anomalias detectadas"
                : "Nenhuma anomalia — sistema operando normalmente");

    return new AiResponse("rule-engine", analysis, data);
  }

  private AiResponse buildRuleBasedRecommendations(Map<String, Object> context, String tenantId) {
    List<String> recommendations = new ArrayList<>();
    int policyCount = ((Number) context.getOrDefault("policyCount", 0)).intValue();
    int enabledFlags = ((Number) context.getOrDefault("enabledFlags", 0)).intValue();

    if (policyCount == 0) {
      recommendations.add("CRITICAL: Configure ao menos uma política ABAC para segurança adequada");
    }
    if (enabledFlags > 20) {
      recommendations.add(
          "MEDIUM: Revise feature flags — %d ativas é acima do recomendado"
              .formatted(enabledFlags));
    }
    if (policyCount > 0 && policyCount < 3) {
      recommendations.add("LOW: Considere políticas por região e por plano para granularidade");
    }
    if (recommendations.isEmpty()) {
      recommendations.add("INFO: Governança adequada — mantenha revisões periódicas");
    }

    String text =
        "## Recomendações de Governança\n**Motor**: Rule Engine\n\n"
            + String.join("\n", recommendations.stream().map(r -> "- " + r).toList());

    return new AiResponse("rule-engine", text, context);
  }

  private AiResponse buildRuleBasedChatResponse(
      String message, AnalyticsService.SummaryResponse summary) {
    long active = summary.tenants().byStatus().getOrDefault("ACTIVE", 0L);
    long totalTenants = summary.tenants().total();
    long totalPolicies = summary.policies().total();
    long enabledFlags = summary.flags().enabled();
    long totalFlags = summary.flags().total();
    long audit24h = summary.audit().last24h();
    long audit7d = summary.audit().last7d();

    String lower = message.toLowerCase();
    String answer;

    if (matchesAny(
        lower,
        "olá",
        "ola",
        "oi",
        "hey",
        "como vai",
        "bom dia",
        "boa tarde",
        "boa noite",
        "hello",
        "hi")) {
      answer =
          String.format(
              "Olá! Sou o assistente de governança do Fluxe B2B Suite.\n\n"
                  + "**Resumo rápido do sistema:**\n"
                  + "- %d tenants ativos de %d cadastrados\n"
                  + "- %d políticas ABAC configuradas\n"
                  + "- %d feature flags ativas de %d\n"
                  + "- %d eventos de auditoria nas últimas 24h\n\n"
                  + "Como posso ajudar? Pergunte sobre tenants, políticas, auditoria, segurança ou flags.",
              active, totalTenants, totalPolicies, enabledFlags, totalFlags, audit24h);

    } else if (matchesAny(lower, "tenant", "inquilino", "cliente", "locatário")) {
      var byPlan = summary.tenants().byPlan();
      var byRegion = summary.tenants().byRegion();
      answer =
          String.format(
              "**Tenants — Visão Geral**\n\n"
                  + "- Total: %d | Ativos: %d | Inativos: %d\n"
                  + "- Por plano: %s\n"
                  + "- Por região: %s\n\n"
                  + "%s",
              totalTenants,
              active,
              totalTenants - active,
              byPlan.isEmpty() ? "N/A" : byPlan.toString(),
              byRegion.isEmpty() ? "N/A" : byRegion.toString(),
              active < totalTenants
                  ? "⚠ Existem tenants inativos. Considere uma campanha de reativação."
                  : "Todos os tenants estão ativos.");

    } else if (matchesAny(lower, "politic", "policy", "abac", "rbac", "permiss", "acesso")) {
      var byEffect = summary.policies().byEffect();
      answer =
          String.format(
              "**Políticas de Governança**\n\n"
                  + "- Total ativas: %d\n"
                  + "- Por efeito: %s\n\n"
                  + "%s",
              totalPolicies,
              byEffect.isEmpty() ? "N/A" : byEffect.toString(),
              totalPolicies == 0
                  ? "⚠ ATENÇÃO: Nenhuma política ABAC configurada. O sistema opera com RBAC básico."
                  : totalPolicies < 3
                      ? "💡 Considere criar políticas por região e plano para maior granularidade."
                      : "Governança configurada adequadamente.");

    } else if (matchesAny(lower, "audit", "log", "evento", "rastreamento", "trilha")) {
      var topActions = summary.audit().topActions();
      StringBuilder sb = new StringBuilder();
      sb.append("**Auditoria**\n\n");
      sb.append(String.format("- Últimas 24h: %d eventos\n", audit24h));
      sb.append(String.format("- Últimos 7 dias: %d eventos\n\n", audit7d));
      if (!topActions.isEmpty()) {
        sb.append("**Top ações (7d):**\n");
        topActions.forEach(a -> sb.append(String.format("- `%s`: %d\n", a.action(), a.count())));
      }
      answer = sb.toString();

    } else if (matchesAny(lower, "flag", "feature", "toggle", "funcionalidade")) {
      answer =
          String.format(
              "**Feature Flags**\n\n" + "- Total: %d | Ativas: %d | Desativadas: %d\n\n" + "%s",
              totalFlags,
              enabledFlags,
              summary.flags().disabled(),
              enabledFlags > 20
                  ? "⚠ Muitas flags ativas. Revise flags antigas que podem virar permanentes."
                  : "Número de flags dentro do esperado.");

    } else if (matchesAny(lower, "segur", "security", "risco", "ameaça", "vulnerab", "ataque")) {
      answer =
          String.format(
              "**Segurança — Resumo**\n\n"
                  + "- Políticas ABAC: %d %s\n"
                  + "- Eventos de auditoria (24h): %d\n"
                  + "- Tenants ativos: %d\n\n"
                  + "Para análise detalhada de segurança, use o botão **Analisar Auditoria** "
                  + "ou pergunte sobre anomalias.",
              totalPolicies,
              totalPolicies == 0 ? "(⚠ configure políticas)" : "(OK)",
              audit24h,
              active);

    } else if (matchesAny(lower, "anomal", "suspeito", "estranho", "irregular")) {
      var anomalies = analyticsService.detectAnomalies();
      if (anomalies.anomalies().isEmpty()) {
        answer = "Nenhuma anomalia detectada nas últimas 24 horas. Sistema operando normalmente.";
      } else {
        StringBuilder sb = new StringBuilder("**Anomalias Detectadas**\n\n");
        anomalies
            .anomalies()
            .forEach(
                a ->
                    sb.append(
                        String.format(
                            "- [%s] %s — ator: `%s`, %d ocorrências em %s\n",
                            a.severity().toUpperCase(),
                            a.type(),
                            a.actor(),
                            a.count(),
                            a.window())));
        answer = sb.toString();
      }

    } else if (matchesAny(lower, "ajuda", "help", "o que", "pode fazer", "comando", "funciona")) {
      answer =
          "**O que posso fazer:**\n\n"
              + "- **Tenants** — status, planos, regiões\n"
              + "- **Políticas** — ABAC/RBAC, efeitos, recomendações\n"
              + "- **Auditoria** — eventos recentes, top ações\n"
              + "- **Feature Flags** — ativas, desativadas, higiene\n"
              + "- **Segurança** — avaliação, anomalias, riscos\n"
              + "- **Anomalias** — detecção em tempo real\n\n"
              + "Também pode usar os botões ao lado: **Analisar Auditoria**, **Recomendações** e **Insights**.";

    } else {
      answer =
          String.format(
              "Entendi sua pergunta: *\"%s\"*\n\n"
                  + "No modo Rule Engine, respondo sobre dados do sistema. "
                  + "Aqui está o que sei agora:\n\n"
                  + "- %d tenants (%d ativos)\n"
                  + "- %d políticas ABAC\n"
                  + "- %d flags ativas\n"
                  + "- %d eventos de auditoria (24h)\n\n"
                  + "Pergunte sobre: **tenants**, **políticas**, **auditoria**, **flags**, "
                  + "**segurança** ou **anomalias**.\n\n"
                  + "> Para respostas livres com linguagem natural, configure `OPENAI_API_KEY`.",
              message, totalTenants, active, totalPolicies, enabledFlags, audit24h);
    }

    return new AiResponse("rule-engine", answer, Map.of("question", message));
  }

  private static boolean matchesAny(String text, String... keywords) {
    for (String kw : keywords) {
      if (text.contains(kw)) return true;
    }
    return false;
  }

  public record AiResponse(
      String engine, String content, @JsonProperty("context") Map<String, Object> metadata) {}

  public record Insight(String severity, String title, String description) {}
}
