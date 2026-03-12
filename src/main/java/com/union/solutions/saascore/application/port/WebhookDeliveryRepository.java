package com.union.solutions.saascore.application.port;

import com.union.solutions.saascore.domain.WebhookDelivery;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface WebhookDeliveryRepository {

  WebhookDelivery save(WebhookDelivery delivery);

  List<WebhookDelivery> findPendingReadyForDelivery(Instant now, Pageable pageable);
}
