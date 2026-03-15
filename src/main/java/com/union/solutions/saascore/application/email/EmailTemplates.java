package com.union.solutions.saascore.application.email;

public final class EmailTemplates {

  private EmailTemplates() {}

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
          <p style="color: #999; font-size: 12px;">Fluxe B2B Suite</p>
        </body>
        </html>
        """
        .formatted(escapeHtml(userName), escapeHtml(tenantName));
  }

  public static String inviteEmail(String userName, String tenantName, String inviteUrl) {
    return inviteEmail(userName, tenantName, inviteUrl, null);
  }

  public static String inviteEmail(
      String userName, String tenantName, String inviteUrl, String temporaryPassword) {
    String passwordBlock =
        temporaryPassword != null && !temporaryPassword.isBlank()
            ? """
          <p><strong>Sua senha temporária:</strong> <code style="background: #f0f0f0; padding: 4px 8px; border-radius: 4px;">%s</code></p>
          <p style="color: #666; font-size: 13px;">Faça login com seu e-mail e esta senha. Recomendamos alterar a senha após o primeiro acesso.</p>
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
          <p style="color: #999; font-size: 12px;">Fluxe B2B Suite</p>
        </body>
        </html>
        """
        .formatted(
            escapeHtml(userName),
            escapeHtml(tenantName),
            passwordBlock,
            escapeHtml(inviteUrl));
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
          <p style="color: #999; font-size: 12px;">Fluxe B2B Suite</p>
        </body>
        </html>
        """
        .formatted(escapeHtml(userName), escapeHtml(resetUrl));
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
