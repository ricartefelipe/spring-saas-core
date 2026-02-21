package com.yourorg.saascore.application.outbox;

import com.yourorg.saascore.application.port.OutboxPublisher;
import com.yourorg.saascore.adapters.out.persistence.OutboxEventEntity;
import com.yourorg.saascore.adapters.out.persistence.OutboxEventJpaRepository;
import com.yourorg.saascore.domain.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final OutboxEventJpaRepository outboxRepo;
    private final OutboxPublisher outboxPublisher;
    private final OutboxDispatcher self;

    private final int batchSize;
    private final int lockTtlSeconds;
    private final int retryMax;
    private final String instanceId;

    public OutboxDispatcher(
            OutboxEventJpaRepository outboxRepo,
            OutboxPublisher outboxPublisher,
            @Lazy OutboxDispatcher self,
            @Value("${app.outbox.batch-size:50}") int batchSize,
            @Value("${app.outbox.lock-ttl-seconds:60}") int lockTtlSeconds,
            @Value("${app.outbox.retry-max:5}") int retryMax) {
        this.outboxRepo = outboxRepo;
        this.outboxPublisher = outboxPublisher;
        this.self = self;
        this.batchSize = batchSize;
        this.lockTtlSeconds = lockTtlSeconds;
        this.retryMax = retryMax;
        this.instanceId = UUID.randomUUID().toString().substring(0, 8);
    }

    @Scheduled(fixedDelayString = "${app.outbox.dispatch-interval-ms:5000}")
    public void dispatch() {
        Instant lockBefore = Instant.now().minusSeconds(lockTtlSeconds);
        List<OutboxEventEntity> pending = self.findPending(lockBefore);
        for (OutboxEventEntity entity : pending) {
            Optional<OutboxEventEntity> claimed = self.claimOne(entity.getId(), lockBefore);
            claimed.ifPresent(this::publishAndUpdate);
        }
    }

    @Transactional(readOnly = true)
    public List<OutboxEventEntity> findPending(Instant lockBefore) {
        return outboxRepo.findPendingUnlocked(lockBefore, PageRequest.of(0, batchSize));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<OutboxEventEntity> claimOne(UUID id, Instant lockBefore) {
        int updated = outboxRepo.tryLock(id, Instant.now(), instanceId, lockBefore);
        if (updated == 1) {
            return outboxRepo.findById(id);
        }
        return Optional.empty();
    }

    private void publishAndUpdate(OutboxEventEntity entity) {
        boolean published = outboxPublisher.publish(entity.toDomain());
        if (published) {
            self.markSent(entity.getId());
        } else {
            self.handleFailure(entity.getId());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(UUID id) {
        OutboxEventEntity e = outboxRepo.findById(id).orElse(null);
        if (e != null) {
            e.setStatus(OutboxEvent.OutboxStatus.SENT);
            e.setSentAt(Instant.now());
            e.setLockedAt(null);
            e.setLockedBy(null);
            outboxRepo.save(e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailure(UUID id) {
        OutboxEventEntity e = outboxRepo.findById(id).orElse(null);
        if (e == null) return;
        e.setRetries(e.getRetries() + 1);
        e.setLockedAt(null);
        e.setLockedBy(null);
        if (e.getRetries() >= retryMax) {
            e.setStatus(OutboxEvent.OutboxStatus.FAILED);
            log.warn("Outbox event marked FAILED after {} retries id={} type={}", retryMax, e.getId(), e.getType());
        }
        outboxRepo.save(e);
    }
}
