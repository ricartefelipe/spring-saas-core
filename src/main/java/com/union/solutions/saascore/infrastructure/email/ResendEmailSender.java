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
import org.springframework.web.client.RestTemplate;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "resend")
public class ResendEmailSender implements EmailSender {

  private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);

  private final RestTemplate restTemplate;
  private final String apiKey;
  private final String fromAddress;

  public ResendEmailSender(
      @Value("${app.email.resend-api-key:}") String apiKey,
      @Value("${app.email.from:noreply@fluxe.com.br}") String fromAddress) {
    this.apiKey = apiKey;
    this.fromAddress = fromAddress;
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
    restTemplate.postForEntity("https://api.resend.com/emails", request, String.class);

    log.info("Email sent via Resend to={} subject={}", to, subject);
  }
}
