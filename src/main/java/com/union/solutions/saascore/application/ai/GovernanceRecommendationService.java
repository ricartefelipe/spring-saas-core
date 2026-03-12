package com.union.solutions.saascore.application.ai;

import com.union.solutions.saascore.adapters.out.persistence.AuditLogJpaRepository;
import com.union.solutions.saascore.application.billing.SubscriptionUseCase;
import com.union.solutions.saascore.application.port.UserRepository;
import com.union.solutions.saascore.application.service.FeatureFlagService;
import com.union.solutions.saascore.application.service.PolicyService;
import com.union.solutions.saascore.application.tenant.TenantUseCase;
import com.union.solutions.saascore.application.webhook.WebhookUseCase;
import com.union.solutions.saascore.domain.FeatureFlag;
import com.union.solutions.saascore.domain.Policy;
import com.union.solutions.saascore.domain.Subscription;
import com.union.solutions.saascore.domain.Tenant;
import com.union.solutions.saascore.domain.User;
import com.union.solutions.saascore.domain.WebhookEndpoint;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GovernanceRecommendationService {

  private static final Logger log = LoggerFactory.getLogger(GovernanceRecommendationService.class);

  private final TenantUseCase tenantUseCase;
  private final PolicyService policyService;
  private final FeatureFlagService featureFlagService;
  private final AuditLogJpaRepository auditRepo;
  private final UserRepository userRepo;
  private final SubscriptionUseCase subscriptionUseCase;
  private final WebhookUseCase webhookUseCase;

  public GovernanceRecommendationService(
      TenantUseCase tenantUseCase,
      PolicyService policyService,
      FeatureFlagService featureFlagService,
      AuditLogJpaRepository auditRepo,
      UserRepository userRepo,
      SubscriptionUseCase subscriptionUseCase,
      WebhookUseCase webhookUseCase) {
    this.tenantUseCase = tenantUseCase;
    this.policyService = policyService;
    this.featureFlagService = featureFlagService;
    this.auditRepo = auditRepo;
    this.userRepo = userRepo;
    this.subscriptionUseCase = subscriptionUseCase;
    this.webhookUseCase = webhookUseCase;
  }

  @Transactional(readOnly = true)
  public GovernanceReport analyzeGovernance(UUID tenantId) {
    log.debug("Analyzing governance for tenant={}", tenantId);

    Tenant tenant =
        tenantUseCase
            .getById(tenantId)
            .orElseThrow(
                () -> new IllegalArgumentException("Tenant not found: " + tenantId));

    List<Recommendation> recommendations = new ArrayList<>();

    checkPolicies(tenant, recommendations);
    checkFeatureFlags(tenantId, recommendations);
    checkAudit(tenantId, recommendations);
    checkUsers(tenantId, recommendations);
    checkWebhooks(tenantId, recommendations);
    checkSubscription(tenantId, recommendations);
    checkAccessDeniedRate(tenantId, recommendations);

    int score = calculateScore(recommendations);

    return new GovernanceReport(tenantId, score, recommendations, Instant.now());
  }

  private void checkPolicies(Tenant tenant, List<Recommendation> recommendations) {
    List<Policy> applicable =
        policyService.getApplicablePolicies(tenant.getPlan(), tenant.getRegion());
    long totalActive = policyService.countActive();

    if (totalActive == 0) {
      recommendations.add(
          new Recommendation(
              "security",
              "critical",
              "Nenhuma política ABAC definida",
              "O sistema não possui nenhuma política ABAC configurada. "
                  + "Isso significa que o controle de acesso opera apenas com RBAC básico, "
                  + "sem granularidade por plano ou região.",
              "Crie pelo menos uma política ABAC via POST /v1/policies"));
    } else if (applicable.isEmpty()) {
      recommendations.add(
          new Recommendation(
              "security",
              "high",
              "Nenhuma política aplicável ao tenant",
              String.format(
                  "Existem %d políticas no sistema, mas nenhuma se aplica ao plano '%s' / região '%s' deste tenant.",
                  totalActive, tenant.getPlan(), tenant.getRegion()),
              "Revise os filtros de plano e região das políticas existentes"));
    }

    boolean hasDeny = applicable.stream().anyMatch(p -> p.getEffect() == Policy.Effect.DENY);
    if (!hasDeny && !applicable.isEmpty()) {
      recommendations.add(
          new Recommendation(
              "security",
              "medium",
              "Sem políticas DENY configuradas",
              "Todas as políticas aplicáveis são do tipo ALLOW. "
                  + "Políticas DENY têm precedência e são recomendadas para restrições explícitas.",
              "Adicione pelo menos uma política DENY para operações sensíveis"));
    }
  }

  private void checkFeatureFlags(UUID tenantId, List<Recommendation> recommendations) {
    List<FeatureFlag> flags = featureFlagService.listByTenant(tenantId);

    if (flags.isEmpty()) {
      return;
    }

    long enabled = flags.stream().filter(FeatureFlag::isEnabled).count();

    if (enabled == 0) {
      recommendations.add(
          new Recommendation(
              "configuration",
              "medium",
              "Todas as feature flags desativadas",
              String.format(
                  "O tenant possui %d feature flags, mas todas estão desativadas. "
                      + "Isso pode indicar uma configuração incompleta.",
                  flags.size()),
              "Revise e ative as feature flags relevantes para o tenant"));
    }

    if (enabled > 20) {
      recommendations.add(
          new Recommendation(
              "hygiene",
              "low",
              "Excesso de feature flags ativas",
              String.format(
                  "%d flags ativas. Flags antigas devem ser promovidas a funcionalidades permanentes.",
                  enabled),
              "Audite flags com mais de 30 dias e promova ou remova as obsoletas"));
    }
  }

  private void checkAudit(UUID tenantId, List<Recommendation> recommendations) {
    Instant since30d = Instant.now().minus(30, ChronoUnit.DAYS);
    long auditCount = auditRepo.countSince(since30d);

    if (auditCount == 0) {
      recommendations.add(
          new Recommendation(
              "compliance",
              "high",
              "Sem registros de auditoria",
              "Nenhum evento de auditoria registrado nos últimos 30 dias. "
                  + "A auditoria é essencial para compliance (LGPD/GDPR) e investigação de incidentes.",
              "Verifique se o AuditLogger está configurado e operacional"));
    }
  }

  private void checkUsers(UUID tenantId, List<Recommendation> recommendations) {
    long userCount = userRepo.countByTenantId(tenantId);

    if (userCount <= 1) {
      recommendations.add(
          new Recommendation(
              "operations",
              "medium",
              "Tenant com usuário único",
              "Apenas 1 usuário cadastrado. "
                  + "Isso cria um ponto único de falha para a operação do tenant.",
              "Convide membros da equipe via POST /v1/users/register"));
    }

    if (userCount > 1) {
      List<User> users = userRepo.findByTenantId(tenantId);
      long adminCount = users.stream().flatMap(u -> u.getRoles().stream()).filter("admin"::equals).count();

      if (adminCount == 0) {
        recommendations.add(
            new Recommendation(
                "security",
                "high",
                "Nenhum administrador definido",
                "O tenant não possui nenhum usuário com role `admin`. "
                    + "Operações administrativas podem ficar sem responsável.",
                "Atribua a role `admin` a pelo menos um usuário"));
      }
    }
  }

  private void checkWebhooks(UUID tenantId, List<Recommendation> recommendations) {
    List<WebhookEndpoint> webhooks = webhookUseCase.listByTenant(tenantId);

    if (webhooks.isEmpty()) {
      recommendations.add(
          new Recommendation(
              "integration",
              "low",
              "Sem webhooks configurados",
              "O tenant não possui webhooks de integração. "
                  + "Webhooks permitem reagir a eventos em tempo real (ex.: notificações, sincronização).",
              "Configure webhooks via POST /v1/webhooks"));
    }
  }

  private void checkSubscription(UUID tenantId, List<Recommendation> recommendations) {
    try {
      Subscription sub = subscriptionUseCase.getCurrentSubscription(tenantId);
      long userCount = userRepo.countByTenantId(tenantId);

      boolean isFreeOrTrial =
          "free".equalsIgnoreCase(sub.getPlanSlug())
              || sub.getStatus() == Subscription.SubscriptionStatus.TRIAL;

      if (isFreeOrTrial && userCount > 5) {
        recommendations.add(
            new Recommendation(
                "billing",
                "medium",
                "Plano gratuito/trial com muitos usuários",
                String.format(
                    "O tenant possui %d usuários no plano '%s'. "
                        + "Planos pagos oferecem maior capacidade e recursos avançados.",
                    userCount, sub.getPlanSlug()),
                "Considere upgrade para um plano pago via POST /v1/billing/subscriptions/upgrade"));
      }

      if (sub.getStatus() == Subscription.SubscriptionStatus.PAST_DUE) {
        recommendations.add(
            new Recommendation(
                "billing",
                "critical",
                "Pagamento pendente",
                "A assinatura está com pagamento atrasado. "
                    + "O serviço pode ser suspenso após o período de carência.",
                "Regularize o pagamento para evitar suspensão"));
      }
    } catch (IllegalStateException e) {
      recommendations.add(
          new Recommendation(
              "billing",
              "high",
              "Sem assinatura ativa",
              "O tenant não possui uma assinatura ativa. "
                  + "Isso pode limitar o acesso a recursos da plataforma.",
              "Inicie um trial ou contrate um plano via POST /v1/billing/subscriptions/trial"));
    }
  }

  private void checkAccessDeniedRate(UUID tenantId, List<Recommendation> recommendations) {
    Instant since7d = Instant.now().minus(7, ChronoUnit.DAYS);

    List<Object[]> deniedSpikes = auditRepo.findAccessDeniedSpikes(since7d, 5);
    if (!deniedSpikes.isEmpty()) {
      long totalDenied = deniedSpikes.stream().mapToLong(row -> ((Number) row[1]).longValue()).sum();
      recommendations.add(
          new Recommendation(
              "security",
              "high",
              "Taxa alta de ACCESS_DENIED",
              String.format(
                  "%d eventos de acesso negado nos últimos 7 dias. "
                      + "Isso pode indicar tentativas de acesso indevido ou políticas restritivas demais.",
                  totalDenied),
              "Revise as políticas ABAC e os logs de auditoria para entender as negações"));
    }
  }

  private int calculateScore(List<Recommendation> recommendations) {
    int score = 100;
    for (Recommendation rec : recommendations) {
      score -=
          switch (rec.severity()) {
            case "critical" -> 25;
            case "high" -> 15;
            case "medium" -> 10;
            case "low" -> 5;
            default -> 0;
          };
    }
    return Math.max(0, score);
  }

  public record GovernanceReport(
      UUID tenantId, int score, List<Recommendation> recommendations, Instant analyzedAt) {}

  public record Recommendation(
      String category, String severity, String title, String description, String action) {}
}
