package com.union.solutions.saascore.application.email;

import java.text.Normalizer;
import java.util.Locale;

public final class EmailTemplates {

  public static final String PRODUCT_DISPLAY_NAME = "Fluxe B2B Suite";

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

  /** Última garantia: o corpo do e-mail nunca deve exibir "System". */
  private static String ensureProductDisplayName(String displayName) {
    if (displayName == null || displayName.isBlank()) return PRODUCT_DISPLAY_NAME;
    String lower = displayName.trim().toLowerCase(Locale.ROOT);
    if (lower.equals("system") || lower.equals("sistema")) return PRODUCT_DISPLAY_NAME;
    return displayName.trim();
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
      String userName, String tenantName, String inviteUrl, String temporaryPassword) {
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
            escapeHtml(ensureProductDisplayName(productNameForInviteDisplay(tenantName))),
            passwordBlock,
            escapeHtml(inviteUrl),
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
