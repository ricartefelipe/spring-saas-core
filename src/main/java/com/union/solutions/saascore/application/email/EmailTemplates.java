package com.union.solutions.saascore.application.email;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

public final class EmailTemplates {

  public static final String PRODUCT_DISPLAY_NAME = "Fluxe B2B Suite";

  /** Mesmo UUID que o tenant de plataforma no Core (Admin Console). */
  private static final UUID PLATFORM_TENANT_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  private EmailTemplates() {}

  /**
   * No convite, "System"/"Sistema" (nome técnico do tenant) deve aparecer sempre como "Fluxe B2B
   * Suite".
   */
  static String productNameForInviteDisplay(String tenantLabel) {
    if (tenantLabel == null || tenantLabel.isBlank()) {
      return PRODUCT_DISPLAY_NAME;
    }
    String n =
        Normalizer.normalize(tenantLabel.trim(), Normalizer.Form.NFKC)
            .replaceAll("[\u200B-\u200D\uFEFF]", "")
            .trim();
    String lower = n.toLowerCase(Locale.ROOT);
    if (lower.equals("system") || lower.equals("sistema")) {
      return PRODUCT_DISPLAY_NAME;
    }
    return n.isEmpty() ? PRODUCT_DISPLAY_NAME : tenantLabel.trim();
  }

  /**
   * Nome a mostrar em «participar de X»: tenant de plataforma → sempre produto; System/Sistema →
   * produto; caso contrário → nome da empresa na BD.
   */
  private static String organizationNameForInvite(UUID inviteTenantId, String tenantNameFromDb) {
    if (inviteTenantId != null && PLATFORM_TENANT_ID.equals(inviteTenantId)) {
      return PRODUCT_DISPLAY_NAME;
    }
    String normalized = productNameForInviteDisplay(tenantNameFromDb);
    String lower = normalized.toLowerCase(Locale.ROOT);
    if (lower.equals("system") || lower.equals("sistema")) {
      return PRODUCT_DISPLAY_NAME;
    }
    return normalized;
  }

  public static String welcomeEmail(String userName, String tenantName) {
    return """
        <!DOCTYPE html>
        <html lang="pt-BR">
        <head><meta charset="UTF-8"></head>
        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
          <h2 style="color: #333;">Bem-vindo(a), %s!</h2>
          <p>Sua conta em <strong>%s</strong> foi ativada com sucesso.</p>
          <p>Agora você pode acessar a plataforma e começar a usar todos os recursos disponíveis.</p>
          <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;">
          <p style="color: #999; font-size: 12px;">%s</p>
        </body>
        </html>
        """
        .formatted(escapeHtml(userName), escapeHtml(tenantName), PRODUCT_DISPLAY_NAME);
  }

  public static String inviteEmail(
      UUID inviteTenantId,
      String userName,
      String tenantNameFromDatabase,
      String inviteUrl,
      String temporaryPassword) {
    String passwordBlock =
        temporaryPassword != null && !temporaryPassword.isBlank()
            ? """
          <div style="background:#fef3c7;border:2px solid #d97706;padding:14px 16px;border-radius:8px;margin:16px 0;">
            <p style="margin:0 0 8px 0;font-size:15px;color:#92400e;"><strong>Obrigatório</strong></p>
            <p style="margin:0;font-size:14px;color:#78350f;">Você <strong>deve</strong> definir uma nova senha no primeiro login. Sem isso, o acesso à plataforma permanece bloqueado após entrar.</p>
          </div>
          <p><strong>Sua senha temporária:</strong> <code style="background: #f0f0f0; padding: 4px 8px; border-radius: 4px;">%s</code></p>
          <p style="color: #666; font-size: 13px;">Use seu e-mail e esta senha para entrar; em seguida o sistema pedirá uma nova senha.</p>
          """
                .formatted(escapeHtml(temporaryPassword))
            : "";
    return """
        <!DOCTYPE html>
        <html lang="pt-BR">
        <head><meta charset="UTF-8"></head>
        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
          <h2 style="color: #333;">Olá, %s!</h2>
          <p>Você foi convidado(a) para participar de <strong>%s</strong>.</p>
          %s
          <p>Clique no botão abaixo para acessar a plataforma:</p>
          <p style="text-align: center; margin: 32px 0;">
            <a href="%s"
               style="background: #2563eb; color: #fff; padding: 12px 32px; text-decoration: none; border-radius: 6px; font-weight: bold;">
              Acessar
            </a>
          </p>
          <p style="color: #666; font-size: 13px;">Se você não esperava este convite, pode ignorar este email.</p>
          <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;">
          <p style="color: #999; font-size: 12px;">%s</p>
        </body>
        </html>
        """
        .formatted(
            escapeHtml(userName),
            escapeHtml(organizationNameForInvite(inviteTenantId, tenantNameFromDatabase)),
            passwordBlock,
            escapeHtml(inviteUrl),
            PRODUCT_DISPLAY_NAME);
  }

  /** E-mail dia 3 pós-signup: dica de uso. */
  public static String postSignupDay3Email(String userName, String tenantName) {
    return """
        <!DOCTYPE html>
        <html lang="pt-BR">
        <head><meta charset="UTF-8"></head>
        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
          <h2 style="color: #333;">Olá, %s!</h2>
          <p>Você está há alguns dias em <strong>%s</strong>. Que tal explorar o painel de uso e convidar sua equipe?</p>
          <p>No menu Faturamento você vê quantos usuários estão ativos e pode gerenciar seu plano. Qualquer dúvida, estamos à disposição.</p>
          <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;">
          <p style="color: #999; font-size: 12px;">%s</p>
        </body>
        </html>
        """
        .formatted(escapeHtml(userName), escapeHtml(tenantName), PRODUCT_DISPLAY_NAME);
  }

  /** E-mail dia 7 pós-signup: reengajamento. */
  public static String postSignupDay7Email(String userName, String tenantName) {
    return """
        <!DOCTYPE html>
        <html lang="pt-BR">
        <head><meta charset="UTF-8"></head>
        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
          <h2 style="color: #333;">Olá, %s!</h2>
          <p>Já faz uma semana que você está conosco em <strong>%s</strong>.</p>
          <p>Se ainda não explorou tudo, aproveite para configurar políticas, integrar sistemas e tirar dúvidas na página de ajuda. Estamos aqui para ajudar.</p>
          <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;">
          <p style="color: #999; font-size: 12px;">%s</p>
        </body>
        </html>
        """
        .formatted(escapeHtml(userName), escapeHtml(tenantName), PRODUCT_DISPLAY_NAME);
  }

  /**
   * Lembrete antes do fim do período de trial — CTA para Faturamento (assinatura / cartão).
   *
   * @param daysLeft 3 ou 1 (cópia e assunto adaptados)
   */
  public static String trialEndingReminderEmail(
      String userName, String tenantName, String trialEndsAtDate, String billingUrl, int daysLeft) {
    String heading =
        daysLeft <= 1
            ? "Seu período de avaliação termina amanhã"
            : "Seu período de avaliação termina em %d dias".formatted(daysLeft);
    return """
        <!DOCTYPE html>
        <html lang="pt-BR">
        <head><meta charset="UTF-8"></head>
        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
          <h2 style="color: #333;">Olá, %s!</h2>
          <p>%s</p>
          <p>Organização: <strong>%s</strong>.</p>
          <p>Término do trial: <strong>%s</strong> (UTC).</p>
          <p>Para não perder o acesso, adicione um método de pagamento ou escolha um plano na área de faturamento.</p>
          <p style="text-align: center; margin: 32px 0;">
            <a href="%s"
               style="background: #2563eb; color: #fff; padding: 12px 32px; text-decoration: none; border-radius: 6px; font-weight: bold;">
              Abrir Faturamento
            </a>
          </p>
          <p style="color: #666; font-size: 13px;">Se você já ativou a assinatura, pode ignorar este lembrete.</p>
          <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;">
          <p style="color: #999; font-size: 12px;">%s</p>
        </body>
        </html>
        """
        .formatted(
            escapeHtml(userName),
            escapeHtml(heading),
            escapeHtml(tenantName),
            escapeHtml(trialEndsAtDate),
            escapeHtml(billingUrl),
            PRODUCT_DISPLAY_NAME);
  }

  /** E-mail de reativação para tenant inativo (sem login há N dias). */
  public static String reactivationEmail(String userName, String tenantName, String loginUrl) {
    return """
        <!DOCTYPE html>
        <html lang="pt-BR">
        <head><meta charset="UTF-8"></head>
        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
          <h2 style="color: #333;">Olá, %s!</h2>
          <p>Notamos que faz um tempo que ninguém acessou <strong>%s</strong> na plataforma.</p>
          <p>Seu plano e seus dados continuam disponíveis. Acesse quando quiser para continuar de onde parou.</p>
          <p style="text-align: center; margin: 32px 0;">
            <a href="%s"
               style="background: #2563eb; color: #fff; padding: 12px 32px; text-decoration: none; border-radius: 6px; font-weight: bold;">
              Acessar agora
            </a>
          </p>
          <p style="color: #666; font-size: 13px;">Se precisar de ajuda, responda este e-mail.</p>
          <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;">
          <p style="color: #999; font-size: 12px;">%s</p>
        </body>
        </html>
        """
        .formatted(
            escapeHtml(userName),
            escapeHtml(tenantName),
            escapeHtml(loginUrl),
            PRODUCT_DISPLAY_NAME);
  }

  public static String passwordResetEmail(String userName, String resetUrl) {
    return """
        <!DOCTYPE html>
        <html lang="pt-BR">
        <head><meta charset="UTF-8"></head>
        <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
          <h2 style="color: #333;">Redefinição de Senha</h2>
          <p>Olá, %s. Recebemos uma solicitação para redefinir sua senha.</p>
          <p style="text-align: center; margin: 32px 0;">
            <a href="%s"
               style="background: #2563eb; color: #fff; padding: 12px 32px; text-decoration: none; border-radius: 6px; font-weight: bold;">
              Redefinir Senha
            </a>
          </p>
          <p style="color: #666; font-size: 13px;">Este link expira em 1 hora. Se você não solicitou esta alteração, ignore este email.</p>
          <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;">
          <p style="color: #999; font-size: 12px;">%s</p>
        </body>
        </html>
        """
        .formatted(escapeHtml(userName), escapeHtml(resetUrl), PRODUCT_DISPLAY_NAME);
  }

  private static String escapeHtml(String input) {
    if (input == null) return "";
    return input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
