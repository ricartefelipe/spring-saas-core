package com.union.solutions.saascore.application.ai;

import com.union.solutions.saascore.adapters.out.persistence.AuditLogJpaRepository;
import com.union.solutions.saascore.application.billing.SubscriptionUseCase;
import com.union.solutions.saascore.application.port.UserRepository;
import com.union.solutions.saascore.application.service.AnalyticsService;
import com.union.solutions.saascore.application.service.FeatureFlagService;
import com.union.solutions.saascore.application.service.PolicyService;
import com.union.solutions.saascore.application.tenant.TenantUseCase;
import com.union.solutions.saascore.application.webhook.WebhookUseCase;
import com.union.solutions.saascore.domain.FeatureFlag;
import com.union.solutions.saascore.domain.Policy;
import com.union.solutions.saascore.domain.Subscription;
import com.union.solutions.saascore.domain.Tenant;
import com.union.solutions.saascore.domain.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GovernanceChatbotService {

  private static final Logger log = LoggerFactory.getLogger(GovernanceChatbotService.class);

  private final TenantUseCase tenantUseCase;
  private final PolicyService policyService;
  private final FeatureFlagService featureFlagService;
  private final AuditLogJpaRepository auditRepo;
  private final UserRepository userRepo;
  private final SubscriptionUseCase subscriptionUseCase;
  private final WebhookUseCase webhookUseCase;
  private final AnalyticsService analyticsService;
  private final GovernanceRecommendationService recommendationService;

  public GovernanceChatbotService(
      TenantUseCase tenantUseCase,
      PolicyService policyService,
      FeatureFlagService featureFlagService,
      AuditLogJpaRepository auditRepo,
      UserRepository userRepo,
      SubscriptionUseCase subscriptionUseCase,
      WebhookUseCase webhookUseCase,
      AnalyticsService analyticsService,
      GovernanceRecommendationService recommendationService) {
    this.tenantUseCase = tenantUseCase;
    this.policyService = policyService;
    this.featureFlagService = featureFlagService;
    this.auditRepo = auditRepo;
    this.userRepo = userRepo;
    this.subscriptionUseCase = subscriptionUseCase;
    this.webhookUseCase = webhookUseCase;
    this.analyticsService = analyticsService;
    this.recommendationService = recommendationService;
  }

  @Transactional(readOnly = true)
  public ChatResponse chat(UUID tenantId, String question) {
    Intent intent = detectIntent(question);
    log.debug("Chat intent={} for tenant={} question=\"{}\"", intent, tenantId, question);

    return switch (intent) {
      case GREETING -> handleGreeting();
      case TENANT_STATUS -> handleTenantStatus(tenantId);
      case POLICIES -> handlePolicies(tenantId);
      case FLAGS -> handleFlags(tenantId);
      case AUDIT -> handleAudit(tenantId);
      case USERS -> handleUsers(tenantId);
      case SUBSCRIPTION -> handleSubscription(tenantId);
      case HELP -> handleHelp();
      case HEALTH -> handleHealth();
      case RECOMMENDATIONS -> handleRecommendations(tenantId);
      case UNKNOWN -> handleUnknown(question);
    };
  }

  private Intent detectIntent(String question) {
    String lower =
        question
            .toLowerCase()
            .trim()
            .replaceAll("[áàâã]", "a")
            .replaceAll("[éèê]", "e")
            .replaceAll("[íìî]", "i")
            .replaceAll("[óòôõ]", "o")
            .replaceAll("[úùû]", "u")
            .replaceAll("[ç]", "c");

    if (matchesAny(
        lower,
        "ola",
        "oi",
        "hey",
        "hello",
        "hi",
        "bom dia",
        "boa tarde",
        "boa noite",
        "como vai",
        "tudo bem",
        "e ai",
        "fala")) {
      return Intent.GREETING;
    }
    if (matchesAny(
        lower, "ajuda", "help", "comandos", "o que pode", "como funciona", "instrucoes", "menu")) {
      return Intent.HELP;
    }

    boolean isListAction =
        matchesAny(
            lower,
            "list",
            "mostre",
            "mostrar",
            "mostra",
            "detalh",
            "exib",
            "quais",
            "quero ver",
            "me de",
            "me da",
            "me liste",
            "apresent",
            "traz",
            "traga",
            "conte",
            "fale sobre",
            "informac");

    if (matchesAny(
        lower,
        "tenant",
        "inquilino",
        "organizac",
        "empresa",
        "meu tenant",
        "info tenant",
        "dados do tenant")) {
      return Intent.TENANT_STATUS;
    }
    if (matchesAny(
        lower,
        "politic",
        "policies",
        "policy",
        "permiss",
        "abac",
        "regras de acesso",
        "controle de acesso",
        "governanc")) {
      return Intent.POLICIES;
    }
    if (matchesAny(
        lower, "flag", "feature flag", "toggle", "funcionalidade", "recurso", "habilit")) {
      return Intent.FLAGS;
    }
    if (matchesAny(
        lower, "audit", "log", "trilha", "rastreamento", "evento", "historico", "registro")) {
      return Intent.AUDIT;
    }
    if (matchesAny(lower, "user", "usuario", "equipe", "membro", "team", "pessoa", "colaborador")) {
      return Intent.USERS;
    }
    if (matchesAny(
        lower,
        "subscription",
        "assinatura",
        "plano",
        "billing",
        "cobranca",
        "fatura",
        "pagamento")) {
      return Intent.SUBSCRIPTION;
    }
    if (matchesAny(
        lower,
        "anomali",
        "problema",
        "alerta",
        "incidente",
        "detectou",
        "detectad",
        "irregularidade",
        "suspeito")) {
      return Intent.HEALTH;
    }
    if (matchesAny(
        lower,
        "health",
        "saude",
        "status",
        "sistema",
        "disponibilidade",
        "visao geral",
        "resumo",
        "overview",
        "dashboard")) {
      return Intent.HEALTH;
    }
    if (matchesAny(
        lower, "recomend", "sugest", "melhoria", "otimizar", "melhorar", "dica", "conselho")) {
      return Intent.RECOMMENDATIONS;
    }

    if (isListAction) {
      return Intent.HEALTH;
    }

    return Intent.UNKNOWN;
  }

  private ChatResponse handleTenantStatus(UUID tenantId) {
    if (tenantId == null) {
      return new ChatResponse(
          "Informe o ID do tenant para consultar o status. "
              + "Verifique se você está autenticado com um token que contenha o `tid`.",
          "tenant_status",
          List.of("Autentique-se com um JWT que inclua o campo `tid`"));
    }

    return tenantUseCase
        .getById(tenantId)
        .map(
            tenant -> {
              String answer =
                  String.format(
                      "## Tenant: %s\n\n"
                          + "| Campo | Valor |\n|---|---|\n"
                          + "| **ID** | `%s` |\n"
                          + "| **Nome** | %s |\n"
                          + "| **Plano** | %s |\n"
                          + "| **Região** | %s |\n"
                          + "| **Status** | %s |\n"
                          + "| **Criado em** | %s |\n"
                          + "| **Atualizado em** | %s |",
                      tenant.getName(),
                      tenant.getId(),
                      tenant.getName(),
                      tenant.getPlan(),
                      tenant.getRegion(),
                      formatStatus(tenant.getStatus()),
                      tenant.getCreatedAt(),
                      tenant.getUpdatedAt());

              List<String> suggestions = new ArrayList<>();
              if (tenant.getStatus() == Tenant.TenantStatus.SUSPENDED) {
                suggestions.add("Tenant suspenso — entre em contato com o suporte para reativação");
              }
              if ("free".equalsIgnoreCase(tenant.getPlan())) {
                suggestions.add("Considere fazer upgrade do plano para acessar mais recursos");
              }
              return new ChatResponse(answer, "tenant_status", suggestions);
            })
        .orElse(
            new ChatResponse(
                "Tenant não encontrado com o ID informado.",
                "tenant_status",
                List.of("Verifique se o ID do tenant está correto")));
  }

  private ChatResponse handlePolicies(UUID tenantId) {
    long totalActive = policyService.countActive();

    if (tenantId == null) {
      String answer =
          String.format(
              "## Políticas de Governança\n\n"
                  + "- **Total ativas no sistema**: %d\n\n"
                  + "Para ver as políticas aplicáveis ao seu tenant, autentique-se com um JWT válido.",
              totalActive);
      return new ChatResponse(
          answer, "policies", List.of("Autentique-se para ver políticas específicas do tenant"));
    }

    return tenantUseCase
        .getById(tenantId)
        .map(
            tenant -> {
              List<Policy> applicable =
                  policyService.getApplicablePolicies(tenant.getPlan(), tenant.getRegion());

              StringBuilder sb = new StringBuilder();
              sb.append(
                  String.format(
                      "## Políticas Aplicáveis — %s\n\n"
                          + "**Plano**: %s | **Região**: %s | **Políticas aplicáveis**: %d / %d\n\n",
                      tenant.getName(),
                      tenant.getPlan(),
                      tenant.getRegion(),
                      applicable.size(),
                      totalActive));

              if (applicable.isEmpty()) {
                sb.append(
                    "> Nenhuma política ABAC aplicável a este tenant. "
                        + "O sistema opera com RBAC básico.\n");
              } else {
                sb.append("| Permissão | Efeito | Planos | Regiões | Notas |\n");
                sb.append("|---|---|---|---|---|\n");
                for (Policy p : applicable) {
                  sb.append(
                      String.format(
                          "| `%s` | %s | %s | %s | %s |\n",
                          p.getPermissionCode(),
                          p.getEffect(),
                          p.getAllowedPlans().isEmpty()
                              ? "todos"
                              : String.join(", ", p.getAllowedPlans()),
                          p.getAllowedRegions().isEmpty()
                              ? "todas"
                              : String.join(", ", p.getAllowedRegions()),
                          p.getNotes() != null ? p.getNotes() : "-"));
                }
              }

              List<String> suggestions = new ArrayList<>();
              if (applicable.isEmpty()) {
                suggestions.add("Crie políticas ABAC para controle de acesso granular");
              }
              long denyCount =
                  applicable.stream().filter(p -> p.getEffect() == Policy.Effect.DENY).count();
              if (denyCount == 0 && !applicable.isEmpty()) {
                suggestions.add(
                    "Considere adicionar políticas DENY para restrições explícitas (DENY tem precedência)");
              }
              return new ChatResponse(sb.toString(), "policies", suggestions);
            })
        .orElse(
            new ChatResponse(
                "Tenant não encontrado.", "policies", List.of("Verifique o ID do tenant")));
  }

  private ChatResponse handleFlags(UUID tenantId) {
    if (tenantId == null) {
      long totalFlags = featureFlagService.countActiveFlags();
      return new ChatResponse(
          String.format(
              "## Feature Flags\n\n- **Flags ativas no sistema**: %d\n\n"
                  + "Autentique-se para ver flags do seu tenant.",
              totalFlags),
          "flags",
          List.of("Autentique-se com um JWT que inclua o `tid`"));
    }

    List<FeatureFlag> flags = featureFlagService.listByTenant(tenantId);
    long enabled = flags.stream().filter(FeatureFlag::isEnabled).count();
    long disabled = flags.size() - enabled;

    StringBuilder sb = new StringBuilder();
    sb.append(
        String.format(
            "## Feature Flags do Tenant\n\n"
                + "**Total**: %d | **Ativas**: %d | **Desativadas**: %d\n\n",
            flags.size(), enabled, disabled));

    if (flags.isEmpty()) {
      sb.append("> Nenhuma feature flag configurada para este tenant.\n");
    } else {
      sb.append("| Nome | Estado | Rollout | Roles Permitidos |\n");
      sb.append("|---|---|---|---|\n");
      for (FeatureFlag f : flags) {
        sb.append(
            String.format(
                "| `%s` | %s | %d%% | %s |\n",
                f.getName(),
                f.isEnabled() ? "Ativa" : "Desativada",
                f.getRolloutPercent(),
                f.getAllowedRoles().isEmpty() ? "todos" : String.join(", ", f.getAllowedRoles())));
      }
    }

    List<String> suggestions = new ArrayList<>();
    if (flags.isEmpty()) {
      suggestions.add("Configure feature flags para controle gradual de funcionalidades");
    }
    if (enabled > 20) {
      suggestions.add(
          "Muitas flags ativas — revise flags antigas que podem ser promovidas a permanentes");
    }
    if (disabled > enabled && flags.size() > 3) {
      suggestions.add("Há mais flags desativadas do que ativas — considere limpar flags obsoletas");
    }
    return new ChatResponse(sb.toString(), "flags", suggestions);
  }

  private ChatResponse handleAudit(UUID tenantId) {
    Instant now = Instant.now();
    long last24h = auditRepo.countSince(now.minus(24, ChronoUnit.HOURS));
    long last7d = auditRepo.countSince(now.minus(7, ChronoUnit.DAYS));

    StringBuilder sb = new StringBuilder();
    sb.append("## Auditoria\n\n");
    sb.append(String.format("- **Últimas 24h**: %d eventos\n", last24h));
    sb.append(String.format("- **Últimos 7 dias**: %d eventos\n\n", last7d));

    List<Object[]> topActions = auditRepo.topActionsSince(now.minus(7, ChronoUnit.DAYS), 10);
    if (!topActions.isEmpty()) {
      sb.append("### Top Ações (7 dias)\n\n");
      sb.append("| Ação | Quantidade |\n|---|---|\n");
      for (Object[] row : topActions) {
        sb.append(String.format("| `%s` | %s |\n", row[0], row[1]));
      }
    }

    if (tenantId != null) {
      List<Object[]> tenantActions =
          auditRepo.topActionsForTenantSince(now.minus(7, ChronoUnit.DAYS), tenantId, 5);
      if (!tenantActions.isEmpty()) {
        sb.append("\n### Ações do Tenant (7 dias)\n\n");
        sb.append("| Ação | Quantidade |\n|---|---|\n");
        for (Object[] row : tenantActions) {
          sb.append(String.format("| `%s` | %s |\n", row[0], row[1]));
        }
      }
    }

    List<String> suggestions = new ArrayList<>();
    if (last24h == 0) {
      suggestions.add("Nenhum evento nas últimas 24h — verifique se a auditoria está ativa");
    }
    if (last7d > 10000) {
      suggestions.add("Volume alto de eventos — considere configurar retenção automática");
    }
    return new ChatResponse(sb.toString(), "audit", suggestions);
  }

  private ChatResponse handleUsers(UUID tenantId) {
    if (tenantId == null) {
      return new ChatResponse(
          "Informe o ID do tenant para consultar os usuários.",
          "users",
          List.of("Autentique-se com um JWT válido para ver dados de usuários"));
    }

    long count = userRepo.countByTenantId(tenantId);
    List<User> users = userRepo.findByTenantId(tenantId);

    Map<String, Long> roleDistribution =
        users.stream()
            .flatMap(u -> u.getRoles().stream())
            .collect(Collectors.groupingBy(r -> r, Collectors.counting()));

    Map<String, Long> statusDistribution =
        users.stream()
            .collect(Collectors.groupingBy(u -> u.getStatus().name(), Collectors.counting()));

    StringBuilder sb = new StringBuilder();
    sb.append(String.format("## Usuários do Tenant\n\n**Total**: %d\n\n", count));

    if (!statusDistribution.isEmpty()) {
      sb.append("### Por Status\n\n");
      statusDistribution.forEach(
          (status, c) -> sb.append(String.format("- **%s**: %d\n", status, c)));
      sb.append("\n");
    }

    if (!roleDistribution.isEmpty()) {
      sb.append("### Por Role\n\n");
      roleDistribution.forEach((role, c) -> sb.append(String.format("- `%s`: %d\n", role, c)));
    }

    List<String> suggestions = new ArrayList<>();
    if (count == 1) {
      suggestions.add("Apenas 1 usuário — convide membros da equipe para colaboração");
    }
    if (!roleDistribution.containsKey("admin") && count > 0) {
      suggestions.add("Nenhum usuário com role `admin` — considere atribuir um administrador");
    }
    long adminCount = roleDistribution.getOrDefault("admin", 0L);
    if (adminCount > 3) {
      suggestions.add(
          String.format(
              "%d admins — muitos administradores podem ser um risco de segurança", adminCount));
    }
    return new ChatResponse(sb.toString(), "users", suggestions);
  }

  private ChatResponse handleSubscription(UUID tenantId) {
    if (tenantId == null) {
      return new ChatResponse(
          "Informe o ID do tenant para consultar a assinatura.",
          "subscription",
          List.of("Autentique-se com um JWT válido"));
    }

    try {
      Subscription sub = subscriptionUseCase.getCurrentSubscription(tenantId);

      String answer =
          String.format(
              "## Assinatura\n\n"
                  + "| Campo | Valor |\n|---|---|\n"
                  + "| **Plano** | %s |\n"
                  + "| **Status** | %s |\n"
                  + "| **Período atual** | %s — %s |\n"
                  + "| **Trial termina em** | %s |\n"
                  + "| **Criada em** | %s |",
              sub.getPlanSlug(),
              sub.getStatus(),
              sub.getCurrentPeriodStart(),
              sub.getCurrentPeriodEnd(),
              sub.getTrialEndsAt() != null ? sub.getTrialEndsAt() : "N/A",
              sub.getCreatedAt());

      List<String> suggestions = new ArrayList<>();
      if (sub.getStatus() == Subscription.SubscriptionStatus.TRIAL) {
        suggestions.add("Sua assinatura está em trial — ative-a para evitar perda de acesso");
      }
      if (sub.getStatus() == Subscription.SubscriptionStatus.PAST_DUE) {
        suggestions.add("Pagamento pendente — regularize para evitar suspensão");
      }
      if ("free".equalsIgnoreCase(sub.getPlanSlug())) {
        suggestions.add("Considere fazer upgrade para desbloquear mais recursos");
      }
      return new ChatResponse(answer, "subscription", suggestions);
    } catch (IllegalStateException e) {
      return new ChatResponse(
          "Nenhuma assinatura ativa encontrada para este tenant.",
          "subscription",
          List.of("Inicie um trial ou contrate um plano"));
    }
  }

  private ChatResponse handleHelp() {
    String answer =
        "## Assistente de Governança\n\n"
            + "Posso ajudar com informações sobre o seu tenant e a plataforma. "
            + "Pergunte sobre:\n\n"
            + "| Comando | Descrição |\n|---|---|\n"
            + "| **tenant status** / **meu tenant** | Informações do tenant (nome, plano, região, status) |\n"
            + "| **policies** / **politicas** | Políticas ABAC ativas e aplicáveis |\n"
            + "| **flags** / **feature flags** | Feature flags e seus estados |\n"
            + "| **audit** / **auditoria** | Resumo de eventos de auditoria |\n"
            + "| **users** / **usuarios** | Contagem de usuários e distribuição de roles |\n"
            + "| **subscription** / **assinatura** | Detalhes da assinatura e plano |\n"
            + "| **health** / **status** | Saúde geral do sistema |\n"
            + "| **recommendations** / **recomendacoes** | Sugestões de melhoria de governança |\n"
            + "| **help** / **ajuda** | Esta lista de comandos |\n\n"
            + "> Suporto perguntas em português e inglês.";
    return new ChatResponse(answer, "help", List.of());
  }

  private ChatResponse handleHealth() {
    AnalyticsService.SummaryResponse summary = analyticsService.getSummary();
    AnalyticsService.AnomalyResponse anomalies = analyticsService.detectAnomalies();

    long activeTenants = summary.tenants().byStatus().getOrDefault("ACTIVE", 0L);
    long totalTenants = summary.tenants().total();
    String systemStatus =
        anomalies.anomalies().isEmpty()
            ? "SAUDÁVEL"
            : AnalyticsService.isOnlyInformationalAnomalies(anomalies.anomalies())
                ? "SAUDÁVEL (observações)"
                : "ATENÇÃO";

    StringBuilder sb = new StringBuilder();
    sb.append(String.format("## Status do Fluxe B2B Suite: %s\n\n", systemStatus));
    sb.append(
        String.format(
            "| Indicador | Valor |\n|---|---|\n"
                + "| **Tenants ativos** | %d / %d |\n"
                + "| **Políticas ABAC** | %d |\n"
                + "| **Feature flags ativas** | %d / %d |\n"
                + "| **Eventos (24h)** | %d |\n"
                + "| **Eventos (7d)** | %d |\n"
                + "| **Anomalias** | %d |",
            activeTenants,
            totalTenants,
            summary.policies().total(),
            summary.flags().enabled(),
            summary.flags().total(),
            summary.audit().last24h(),
            summary.audit().last7d(),
            anomalies.anomalies().size()));

    if (!anomalies.anomalies().isEmpty()) {
      boolean onlyInfo = AnalyticsService.isOnlyInformationalAnomalies(anomalies.anomalies());
      sb.append(
          onlyInfo ? "\n\n### Observações de auditoria\n\n" : "\n\n### Anomalias detectadas\n\n");
      for (AnalyticsService.Anomaly a : anomalies.anomalies()) {
        sb.append(
            String.format(
                "- **[%s]** %s — ator: `%s`, %d ocorrências (%s)\n",
                a.severity().toUpperCase(), a.type(), a.actor(), a.count(), a.window()));
      }
    }

    List<String> suggestions = new ArrayList<>();
    if (!anomalies.anomalies().isEmpty()) {
      suggestions.add(
          AnalyticsService.isOnlyInformationalAnomalies(anomalies.anomalies())
              ? "Se notar picos de acesso negado ou burst, investigue com prioridade"
              : "Investigue as anomalias detectadas");
    }
    if (summary.policies().total() == 0) {
      suggestions.add("Configure políticas ABAC para segurança adequada");
    }
    if (activeTenants == 0) {
      suggestions.add("Nenhum tenant ativo — verifique a configuração");
    }
    return new ChatResponse(sb.toString(), "health", suggestions);
  }

  private ChatResponse handleRecommendations(UUID tenantId) {
    if (tenantId == null) {
      return new ChatResponse(
          "Informe o ID do tenant para gerar recomendações de governança.",
          "recommendations",
          List.of("Autentique-se com um JWT válido"));
    }

    GovernanceRecommendationService.GovernanceReport report =
        recommendationService.analyzeGovernance(tenantId);

    StringBuilder sb = new StringBuilder();
    sb.append(
        String.format(
            "## Recomendações de Governança\n\n" + "**Score**: %d/100 | **Verificações**: %d\n\n",
            report.score(), report.recommendations().size()));

    if (report.recommendations().isEmpty()) {
      sb.append("Nenhuma recomendação — governança adequada!\n");
    } else {
      for (GovernanceRecommendationService.Recommendation rec : report.recommendations()) {
        sb.append(
            String.format(
                "### [%s] %s\n**Categoria**: %s\n\n%s\n\n> **Ação**: %s\n\n",
                rec.severity().toUpperCase(),
                rec.title(),
                rec.category(),
                rec.description(),
                rec.action()));
      }
    }

    List<String> suggestions =
        report.recommendations().stream()
            .map(GovernanceRecommendationService.Recommendation::action)
            .toList();
    return new ChatResponse(sb.toString(), "recommendations", suggestions);
  }

  private ChatResponse handleGreeting() {
    AnalyticsService.SummaryResponse summary = analyticsService.getSummary();
    long activeTenants = summary.tenants().byStatus().getOrDefault("ACTIVE", 0L);
    boolean hasAnomalies = !analyticsService.detectAnomalies().anomalies().isEmpty();

    StringBuilder sb = new StringBuilder();
    sb.append("Olá! Tudo bem? Que bom ter você por aqui! \uD83D\uDE0A\n\n");
    sb.append("Sou o assistente de governança do **Fluxe B2B Suite** ");
    sb.append("e estou aqui para te ajudar no que precisar.\n\n");

    if (hasAnomalies) {
      sb.append("Vi que temos algumas **anomalias** no sistema que merecem atenção. ");
      sb.append("Quer que eu detalhe?\n\n");
    } else {
      sb.append(
          String.format(
              "Está tudo tranquilo por aqui — **%d tenants** ativos, "
                  + "**%d políticas** configuradas e nenhuma anomalia detectada.\n\n",
              activeTenants, summary.policies().total()));
    }

    sb.append("No que posso te ajudar? Alguns exemplos:\n\n");
    sb.append("- \uD83C\uDFE2 **\"tenants\"** — ver informações dos tenants\n");
    sb.append("- \uD83D\uDD12 **\"políticas\"** — consultar regras de acesso\n");
    sb.append("- \uD83D\uDCCA **\"auditoria\"** — eventos recentes\n");
    sb.append("- \u2699\uFE0F **\"flags\"** — feature flags ativas\n");
    sb.append("- \uD83D\uDCA1 **\"recomendações\"** — sugestões de melhoria\n\n");
    sb.append("Ou pode perguntar com suas próprias palavras — eu me viro! \uD83D\uDE09");

    List<String> suggestions =
        hasAnomalies
            ? List.of("anomalias", "status", "recomendações")
            : List.of("tenants", "políticas", "auditoria");
    return new ChatResponse(sb.toString(), "greeting", suggestions);
  }

  private ChatResponse handleUnknown(String question) {
    String answer =
        String.format(
            "Não consegui identificar o assunto de: *\"%s\"*\n\n"
                + "Tente perguntar de forma mais direta. Exemplos:\n\n"
                + "- **\"tenants\"** — informações do tenant\n"
                + "- **\"políticas\"** — políticas ABAC\n"
                + "- **\"flags\"** — feature flags\n"
                + "- **\"auditoria\"** — eventos recentes\n"
                + "- **\"usuários\"** — membros da equipe\n"
                + "- **\"status\"** — visão geral do sistema\n"
                + "- **\"anomalias\"** — problemas detectados\n"
                + "- **\"recomendações\"** — sugestões de melhoria\n"
                + "- **\"ajuda\"** — lista completa de comandos",
            question);
    return new ChatResponse(
        answer,
        "unknown",
        List.of("tenants", "políticas", "flags", "auditoria", "status", "anomalias", "ajuda"));
  }

  private static String formatStatus(Tenant.TenantStatus status) {
    return switch (status) {
      case ACTIVE -> "Ativo";
      case SUSPENDED -> "Suspenso";
      case DELETED -> "Deletado";
    };
  }

  private static boolean matchesAny(String text, String... keywords) {
    for (String kw : keywords) {
      if (text.contains(kw)) return true;
    }
    return false;
  }

  public record ChatResponse(String answer, String intent, List<String> suggestions) {}

  private enum Intent {
    GREETING,
    TENANT_STATUS,
    POLICIES,
    FLAGS,
    AUDIT,
    USERS,
    SUBSCRIPTION,
    HELP,
    HEALTH,
    RECOMMENDATIONS,
    UNKNOWN
  }
}
