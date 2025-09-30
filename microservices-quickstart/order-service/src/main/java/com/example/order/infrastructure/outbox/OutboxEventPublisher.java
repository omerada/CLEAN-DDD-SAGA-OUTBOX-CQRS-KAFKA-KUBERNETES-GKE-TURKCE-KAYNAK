package com.example.order.infrastructure.outbox;

import com.example.order.application.port.out.OutboxRepositoryPort;
import com.example.order.domain.outbox.OutboxEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox Event Publisher Service
 * 
 * Responsible for:
 * - Processing pending outbox events
 * - Publishing events to message broker (Kafka)
 * - Handling failures and retries
 * - Cleanup of old published events
 */
@Service
@Transactional
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final OutboxRepositoryPort outboxRepository;
    private final OutboxConfiguration outboxConfig;

    public OutboxEventPublisher(
            OutboxRepositoryPort outboxRepository,
            OutboxConfiguration outboxConfig) {
        this.outboxRepository = outboxRepository;
        this.outboxConfig = outboxConfig;
    }

    /**
     * Process pending outbox events
     */
    @Transactional
    public void processPendingEvents() {
        try {
            int batchSize = outboxConfig.getBatchSize();
            Pageable pageable = PageRequest.of(0, batchSize);

            List<OutboxEvent> pendingEvents = outboxRepository.findPendingEvents(pageable);

            if (pendingEvents.isEmpty()) {
                log.debug("No pending outbox events to process");
                return;
            }

            log.info("Processing {} pending outbox events", pendingEvents.size());

            int successCount = 0;
            int failureCount = 0;

            for (OutboxEvent event : pendingEvents) {
                try {
                    boolean published = publishEvent(event);

                    if (published) {
                        event.markAsPublished();
                        successCount++;
                        log.debug("Successfully published outbox event: {}", event.getId());
                    } else {
                        event.markAsFailed("Publishing failed - unknown error");
                        failureCount++;
                        log.warn("Failed to publish outbox event: {}", event.getId());
                    }

                } catch (Exception e) {
                    event.markAsFailed("Publishing failed: " + e.getMessage());
                    failureCount++;
                    log.error("Error publishing outbox event: {}", event.getId(), e);
                }
            }

            // Bulk save all updates
            outboxRepository.saveAll(pendingEvents);

            log.info("Outbox processing completed - Success: {}, Failed: {}",
                    successCount, failureCount);

        } catch (Exception e) {
            log.error("Error processing pending outbox events", e);
        }
    }

    /**
     * Retry failed events
     */
    @Transactional
    public void retryFailedEvents() {
        try {
            int maxRetries = outboxConfig.getMaxRetries();
            LocalDateTime retryThreshold = LocalDateTime.now()
                    .minus(outboxConfig.getRetryDelay());

            Pageable pageable = PageRequest.of(0, outboxConfig.getRetryBatchSize());

            List<OutboxEvent> retryableEvents = outboxRepository
                    .findRetryableEvents(maxRetries, retryThreshold, pageable);

            if (retryableEvents.isEmpty()) {
                log.debug("No retryable outbox events found");
                return;
            }

            log.info("Retrying {} failed outbox events", retryableEvents.size());

            int retrySuccessCount = 0;
            int retryFailureCount = 0;

            for (OutboxEvent event : retryableEvents) {
                try {
                    // Reset event for retry
                    event.resetForRetry();

                    boolean published = publishEvent(event);

                    if (published) {
                        event.markAsPublished();
                        retrySuccessCount++;
                        log.info("Successfully retried outbox event: {} (attempt {})",
                                event.getId(), event.getRetryCount());
                    } else {
                        event.markAsFailed("Retry failed - unknown error");
                        retryFailureCount++;
                        log.warn("Retry failed for outbox event: {} (attempt {})",
                                event.getId(), event.getRetryCount());
                    }

                } catch (Exception e) {
                    event.markAsFailed("Retry failed: " + e.getMessage());
                    retryFailureCount++;
                    log.error("Error retrying outbox event: {} (attempt {})",
                            event.getId(), event.getRetryCount(), e);
                }
            }

            // Bulk save all updates
            outboxRepository.saveAll(retryableEvents);

            log.info("Outbox retry completed - Success: {}, Failed: {}",
                    retrySuccessCount, retryFailureCount);

        } catch (Exception e) {
            log.error("Error retrying failed outbox events", e);
        }
    }

    /**
     * Cleanup old published events
     */
    @Transactional
    public void cleanupOldEvents() {
        try {
            LocalDateTime cleanupThreshold = LocalDateTime.now()
                    .minus(outboxConfig.getCleanupAge());

            int deletedCount = outboxRepository.deleteOldPublishedEvents(cleanupThreshold);

            if (deletedCount > 0) {
                log.info("Cleaned up {} old published outbox events", deletedCount);
            }

        } catch (Exception e) {
            log.error("Error cleaning up old outbox events", e);
        }
    }

    /**
     * Publish single event (simulated for now)
     * 
     * TODO: Implement actual Kafka publishing
     */
    private boolean publishEvent(OutboxEvent outboxEvent) {
        try {
            // Simulate event publishing - replace with actual Kafka implementation
            log.info("Publishing event: {} - Type: {} - Aggregate: {}",
                    outboxEvent.getId(),
                    outboxEvent.getEventType(),
                    outboxEvent.getAggregateId());

            // Simulate some processing time
            Thread.sleep(10);

            // For now, always return true (successful)
            // In real implementation, this would publish to Kafka and handle errors
            return true;

        } catch (Exception e) {
            log.error("Failed to publish event: {}", outboxEvent.getId(), e);
            return false;
        }
    }
}