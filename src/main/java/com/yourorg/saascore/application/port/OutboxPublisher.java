package com.yourorg.saascore.application.port;

import com.yourorg.saascore.domain.OutboxEvent;

public interface OutboxPublisher {

    boolean publish(OutboxEvent event);
}
