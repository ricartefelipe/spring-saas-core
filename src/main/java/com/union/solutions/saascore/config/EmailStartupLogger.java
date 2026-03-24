package com.union.solutions.saascore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Uma linha nos logs ao subir o Core — essencial para ver no Railway se o provider e credenciais
 * batem com o esperado (sem expor segredos).
 */
@Component
@Order(100)
public class EmailStartupLogger implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(EmailStartupLogger.class);

  @Value("${app.email.provider:log}")
  private String rawProvider;

  @Value("${app.email.resend-api-key:}")
  private String resendKey;

  @Value("${app.email.smtp.host:}")
  private String smtpHost;

  @Value("${app.email.from:}")
  private String from;

  @Override
  public void run(ApplicationArguments args) {
    String trimmedRaw = rawProvider == null ? "" : rawProvider.trim();
    String p = EmailProviderConstants.normalize(rawProvider);
    boolean resendKeyOk = resendKey != null && !resendKey.isBlank();
    boolean smtpHostOk = smtpHost != null && !smtpHost.isBlank();
    String fromSafe = from == null || from.isBlank() ? "(não definido)" : from.trim();

    log.info(
        "=== E-mail (arranque) === EMAIL_PROVIDER_raw={} | providerEfetivo={} | RESEND_API_KEY preenchida={} | SMTP_HOST preenchido={} | EMAIL_FROM={}",
        trimmedRaw.isEmpty() ? "(vazio ou não definido — cai no default log)" : trimmedRaw,
        p,
        resendKeyOk,
        smtpHostOk,
        fromSafe);

    if (!trimmedRaw.isEmpty() && "log".equals(p) && !"log".equalsIgnoreCase(trimmedRaw)) {
      log.error(
          "EMAIL_PROVIDER='{}' não é reconhecido — só são aceites: resend, smtp, log. "
              + "Valores desconhecidos são tratados como log (sem envio real). Corrija o typo no Railway.",
          trimmedRaw);
    }

    if ("log".equals(p)) {
      log.warn(
          "E-mail em modo LOG: nenhum envio real. Para entregar na caixa: EMAIL_PROVIDER=resend (HTTPS, recomendado no Railway) "
              + "ou EMAIL_PROVIDER=smtp (muitos hosts bloqueiam saída SMTP — ver docs/CONVITE-EMAIL-DEPLOY.md).");
    } else if ("resend".equals(p) && !resendKeyOk) {
      log.error(
          "EMAIL_PROVIDER=resend mas RESEND_API_KEY vazia — envio vai falhar na primeira chamada.");
    } else if ("smtp".equals(p) && !smtpHostOk) {
      log.error("EMAIL_PROVIDER=smtp mas SMTP_HOST vazio — envio vai falhar.");
    }
  }
}
