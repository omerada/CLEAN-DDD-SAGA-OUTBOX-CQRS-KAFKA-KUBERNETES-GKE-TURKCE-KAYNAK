package com.example.order.infrastructure.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Outbox Relay Scheduler
 * 
 * Periodically processes outbox events using Spring's @Scheduled annotation
 */
@Component
@ConditionalOnProperty(value = "outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);

    private final OutboxEventPublisher outboxEventPublisher;
    private final OutboxConfiguration outboxConfig;

    public OutboxRelayScheduler(
            OutboxEventPublisher outboxEventPublisher,
            OutboxConfiguration outboxConfig) {
        this.outboxEventPublisher = outboxEventPublisher;
        this.outboxConfig = outboxConfig;
    }

    /**
     * Process pending events - runs every 5 seconds
     */
    @Scheduled(fixedDelayString = "${outbox.processing.interval:5000}")
    public void processPendingEvents() {
        if (!outboxConfig.isEnabled()) {
            return;
        }

        try {
            log.debug("Starting outbox event processing");
            outboxEventPublisher.processPendingEvents();
        } catch (Exception e) {
            log.error("Error in outbox event processing", e);
        }
    }

    /**
     * Retry failed events - runs every 30 seconds
     */
    @Scheduled(fixedDelayString = "${outbox.retry.interval:30000}")
    public void retryFailedEvents() {
        if (!outboxConfig.isEnabled() || !outboxConfig.isRetryEnabled()) {
            return;
        }

        try {
            log.debug("Starting outbox event retry processing");
            outboxEventPublisher.retryFailedEvents();
        } catch (Exception e) {
            log.error("Error in outbox event retry processing", e);
        }
    }

    /**
     * Cleanup old events - runs every hour
     */
    @Scheduled(cron = "${outbox.cleanup.schedule:0 0 * * * *}")
    public void cleanupOldEvents() {
        if (!outboxConfig.isEnabled() || !outboxConfig.isCleanupEnabled()) {
            return;
        }

        try {
            log.debug("Starting outbox event cleanup");
            outboxEventPublisher.cleanupOldEvents();
        } catch (Exception e) {
            log.error("Error in outbox event cleanup", e);
        }
    }
}