package com.union.solutions.saascore.infrastructure.webhook;

import com.union.solutions.saascore.application.port.WebhookDeliveryRepository;
import com.union.solutions.saascore.application.port.WebhookEndpointRepository;
import com.union.solutions.saascore.domain.WebhookDelivery;
import com.union.solutions.saascore.domain.WebhookEndpoint;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class WebhookDeliveryWorker {

  private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryWorker.class);
  private static final int MAX_ATTEMPTS = 5;
  private static final int BATCH_SIZE = 20;

  private final WebhookDeliveryRepository deliveryRepo;
  private final WebhookEndpointRepository endpointRepo;
  private final RestTemplate restTemplate;
  private final int maxAttempts;

  public WebhookDeliveryWorker(
      WebhookDeliveryRepository deliveryRepo,
      WebhookEndpointRepository endpointRepo,
      @Qualifier("webhookRestTemplate") RestTemplate restTemplate,
      @Value("${app.webhook.max-attempts:" + MAX_ATTEMPTS + "}") int maxAttempts) {
    this.deliveryRepo = deliveryRepo;
    this.endpointRepo = endpointRepo;
    this.restTemplate = restTemplate;
    this.maxAttempts = maxAttempts;
  }

  @Scheduled(fixedDelayString = "${app.webhook.dispatch-interval-ms:10000}")
  @Transactional
  public void processPending() {
    List<WebhookDelivery> pending =
        deliveryRepo.findPendingReadyForDelivery(Instant.now(), PageRequest.of(0, BATCH_SIZE));
    for (WebhookDelivery delivery : pending) {
      processOne(delivery);
    }
  }

  private void processOne(WebhookDelivery delivery) {
    Optional<WebhookEndpoint> endpointOpt =
        endpointRepo.findByIdAndTenantId(delivery.getEndpointId(), delivery.getTenantId());
    if (endpointOpt.isEmpty()) {
      markFailed(delivery, null, "Endpoint not found");
      return;
    }
    WebhookEndpoint endpoint = endpointOpt.get();
    if (!endpoint.isActive()) {
      markFailed(delivery, null, "Endpoint inactive");
      return;
    }

    String payload = delivery.getPayload();
    String signature = computeHmacSha256(payload, endpoint.getSecret());
    if (signature == null) {
      markFailed(delivery, null, "Failed to compute signature");
      return;
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Webhook-Signature", "sha256=" + signature);
    HttpEntity<String> entity = new HttpEntity<>(payload, headers);

    try {
      var response =
          restTemplate.exchange(
              URI.create(endpoint.getUrl()),
              HttpMethod.POST,
              entity,
              String.class);

      HttpStatusCode status = response.getStatusCode();
      int code = status.value();
      if (code >= 200 && code < 300) {
        delivery.setStatus(WebhookDelivery.Status.DELIVERED);
        delivery.setResponseCode(code);
        delivery.setUpdatedAt(Instant.now());
        deliveryRepo.save(delivery);
        log.info(
            "Webhook delivered delivery_id={} endpoint_id={} event_type={} response={}",
            delivery.getId(),
            delivery.getEndpointId(),
            delivery.getEventType(),
            code);
      } else {
        handleFailure(delivery, code, "HTTP " + code);
      }
    } catch (RestClientException e) {
      handleFailure(delivery, null, e.getMessage());
    }
  }

  private void handleFailure(WebhookDelivery delivery, Integer responseCode, String reason) {
    int attempts = delivery.getAttempts() + 1;
    delivery.setAttempts(attempts);
    delivery.setResponseCode(responseCode);
    delivery.setUpdatedAt(Instant.now());
    if (attempts >= maxAttempts) {
      delivery.setStatus(WebhookDelivery.Status.FAILED);
      delivery.setNextAttemptAt(null);
      deliveryRepo.save(delivery);
      log.error(
          "Webhook delivery failed permanently delivery_id={} endpoint_id={} event_type={} attempts={} reason={}",
          delivery.getId(),
          delivery.getEndpointId(),
          delivery.getEventType(),
          attempts,
          reason);
    } else {
      long backoffSeconds = (long) Math.pow(2, attempts);
      delivery.setNextAttemptAt(Instant.now().plusSeconds(backoffSeconds));
      deliveryRepo.save(delivery);
      log.warn(
          "Webhook delivery failed, will retry delivery_id={} endpoint_id={} event_type={} attempt={}/{} next_in_s={} reason={}",
          delivery.getId(),
          delivery.getEndpointId(),
          delivery.getEventType(),
          attempts,
          maxAttempts,
          backoffSeconds,
          reason);
    }
  }

  private void markFailed(WebhookDelivery delivery, Integer responseCode, String reason) {
    delivery.setStatus(WebhookDelivery.Status.FAILED);
    delivery.setAttempts(delivery.getAttempts() + 1);
    delivery.setResponseCode(responseCode);
    delivery.setNextAttemptAt(null);
    delivery.setUpdatedAt(Instant.now());
    deliveryRepo.save(delivery);
    log.error(
        "Webhook delivery failed delivery_id={} endpoint_id={} reason={}",
        delivery.getId(),
        delivery.getEndpointId(),
        reason);
  }

  private String computeHmacSha256(String payload, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      log.error("Failed to compute HMAC-SHA256: {}", e.getMessage());
      return null;
    }
  }
}
