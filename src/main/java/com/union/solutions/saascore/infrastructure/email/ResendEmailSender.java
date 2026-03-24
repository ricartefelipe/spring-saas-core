package com.union.solutions.saascore.infrastructure.email;

import com.union.solutions.saascore.application.port.EmailDispatchResult;
import com.union.solutions.saascore.application.port.EmailSender;
import com.union.solutions.saascore.config.ConditionalOnEmailProvider;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@ConditionalOnEmailProvider("resend")
public class ResendEmailSender implements EmailSender {

  private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);
  private static final String RESEND_API_URL = "https://api.resend.com/emails";

  private final RestTemplate restTemplate;
  private final String apiKey;
  private final String fromAddress; // "email@domain" ou "Name <email@domain>"
  private final boolean failOnDeliveryError;

  public ResendEmailSender(
      @Value("${app.email.resend-api-key:}") String apiKey,
      @Value("${app.email.from:noreply@fluxe.com.br}") String fromAddress,
      @Value("${app.email.from-name:}") String fromName,
      @Value("${app.email.fail-on-delivery-error:false}") boolean failOnDeliveryError) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException(
          "app.email.provider=resend requires app.email.resend-api-key (RESEND_API_KEY). "
              + "Set RESEND_API_KEY in environment or use app.email.provider=log for dev.");
    }
    this.apiKey = apiKey.trim();
    String rawFrom =
        fromAddress != null && !fromAddress.isBlank() ? fromAddress.trim() : "noreply@fluxe.com.br";
    this.fromAddress = buildFromAddress(rawFrom, fromName);
    this.failOnDeliveryError = failOnDeliveryError;
    this.restTemplate = new RestTemplate();
  }

  /**
   * Resend aceita "email@domain" ou "Display Name &lt;email@domain&gt;". Se fromName estiver
   * definido e rawFrom não tiver formato "Name &lt;email&gt;", combina.
   */
  private static String buildFromAddress(String rawFrom, String fromName) {
    if (fromName == null || fromName.isBlank()) {
      return rawFrom;
    }
    if (rawFrom.contains("<") && rawFrom.contains(">")) {
      return rawFrom;
    }
    return fromName.trim() + " <" + rawFrom + ">";
  }

  @Override
  public EmailDispatchResult send(String to, String subject, String htmlBody) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(apiKey);
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> body =
        Map.of("from", fromAddress, "to", List.of(to), "subject", subject, "html", htmlBody);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
    try {
      restTemplate.postForEntity(RESEND_API_URL, request, String.class);
      log.info("Email sent via Resend to={} subject={}", to, subject);
      return EmailDispatchResult.accepted();
    } catch (RestClientException e) {
      String msg = e.getMessage() != null ? e.getMessage() : "";
      log.error(
          "Resend email delivery failed. to={} subject={} error={}. "
              + "Domínio não verificado? Vá em resend.com/domains, adicione o domínio do EMAIL_FROM e configure DNS (MX, SPF, DKIM).",
          to,
          subject,
          msg);
      if (!failOnDeliveryError) {
        log.warn(
            "Resend falhou; convite não chegou ao destinatário (fail-on-delivery-error=false). "
                + "Verifique RESEND_API_KEY, domínio em resend.com/domains e que EMAIL_FROM coincide com o domínio verificado.");
        log.warn(
            "Conteúdo HTML do convite (recuperação manual da senha provisória até o Resend corrigir):\n{}",
            htmlBody);
        return EmailDispatchResult.rejectedAfterAttempt();
      }
      throw new IllegalStateException(
          "Email delivery failed. Check RESEND_API_KEY and Resend dashboard (domain verification). "
              + "Details: "
              + msg,
          e);
    }
  }
}
