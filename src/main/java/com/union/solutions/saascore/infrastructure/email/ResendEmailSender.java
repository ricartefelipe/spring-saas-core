package com.union.solutions.saascore.infrastructure.email;

import com.union.solutions.saascore.application.port.EmailSender;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "resend")
public class ResendEmailSender implements EmailSender {

  private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);
  private static final String RESEND_API_URL = "https://api.resend.com/emails";

  private final RestTemplate restTemplate;
  private final String apiKey;
  private final String fromAddress;
  private final boolean failOnDeliveryError;

  public ResendEmailSender(
      @Value("${app.email.resend-api-key:}") String apiKey,
      @Value("${app.email.from:noreply@fluxe.com.br}") String fromAddress,
      @Value("${app.email.fail-on-delivery-error:false}") boolean failOnDeliveryError) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException(
          "app.email.provider=resend requires app.email.resend-api-key (RESEND_API_KEY). "
              + "Set RESEND_API_KEY in environment or use app.email.provider=log for dev.");
    }
    this.apiKey = apiKey.trim();
    this.fromAddress = fromAddress != null ? fromAddress : "noreply@fluxe.com.br";
    this.failOnDeliveryError = failOnDeliveryError;
    this.restTemplate = new RestTemplate();
  }

  @Override
  public void send(String to, String subject, String htmlBody) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(apiKey);
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> body =
        Map.of("from", fromAddress, "to", List.of(to), "subject", subject, "html", htmlBody);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
    try {
      restTemplate.postForEntity(RESEND_API_URL, request, String.class);
      log.info("Email sent via Resend to={} subject={}", to, subject);
    } catch (RestClientException e) {
      String msg = e.getMessage() != null ? e.getMessage() : "";
      boolean isResendRecoverable =
          msg.contains("validation_error")
              || msg.contains("only send testing emails to your own")
              || (msg.contains("not verified") && msg.contains("domain"))
              || msg.contains("domain is not verified");
      log.error(
          "Resend email delivery failed. to={} subject={} error={}. "
              + "Domínio não verificado? Vá em resend.com/domains, adicione o domínio do EMAIL_FROM e configure DNS (MX, SPF, DKIM).",
          to,
          subject,
          msg);
      if (isResendRecoverable || !failOnDeliveryError) {
        log.warn(
            "User created but invite email not sent. Verify domain at resend.com/domains — plan upgrade alone is not enough, domain verification is required.");
        return;
      }
      throw new IllegalStateException(
          "Email delivery failed. Check RESEND_API_KEY and Resend dashboard (domain verification). "
              + "Details: "
              + msg,
          e);
    }
  }
}
