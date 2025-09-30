package com.example.order.application.port.out;

import com.example.order.domain.outbox.OutboxEvent;
import com.example.order.domain.outbox.OutboxEventStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Outbox Repository Port (Secondary Port)
 * 
 * Output boundary for outbox event persistence operations
 */
public interface OutboxRepositoryPort {

    /**
     * Save outbox event
     */
    OutboxEvent save(OutboxEvent outboxEvent);

    /**
     * Save multiple outbox events
     */
    List<OutboxEvent> saveAll(List<OutboxEvent> outboxEvents);

    /**
     * Find event by ID
     */
    Optional<OutboxEvent> findById(String id);

    /**
     * Find pending events for publishing
     */
    List<OutboxEvent> findPendingEvents(Pageable pageable);

    /**
     * Find failed events that can be retried
     */
    List<OutboxEvent> findRetryableEvents(
            int maxRetries,
            LocalDateTime retryThreshold,
            Pageable pageable);

    /**
     * Find events by aggregate
     */
    List<OutboxEvent> findByAggregateTypeAndAggregateId(
            String aggregateType,
            String aggregateId);

    /**
     * Find events for cleanup
     */
    List<OutboxEvent> findEventsForCleanup(LocalDateTime cleanupThreshold);

    /**
     * Count events by status
     */
    long countByStatus(OutboxEventStatus status);

    /**
     * Count events by status and time range
     */
    long countByStatusAndTimeRange(
            OutboxEventStatus status,
            LocalDateTime startTime,
            LocalDateTime endTime);

    /**
     * Delete old published events in bulk
     */
    int deleteOldPublishedEvents(LocalDateTime cleanupThreshold);

    /**
     * Find events by correlation ID for tracing
     */
    List<OutboxEvent> findByCorrelationId(String correlationId);

    /**
     * Find events that might be stuck (pending too long)
     */
    List<OutboxEvent> findStuckEvents(LocalDateTime stuckThreshold);
}