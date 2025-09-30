package com.example.order.infrastructure.adapter.out.persistence;

import com.example.order.domain.outbox.OutboxEvent;
import com.example.order.domain.outbox.OutboxEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA Repository for Outbox Event persistence
 */
@Repository
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, String> {

    /**
     * Find pending events for publishing
     */
    @Query("""
            SELECT e FROM OutboxEvent e
            WHERE e.status = :status
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEvent> findPendingEvents(
            @Param("status") OutboxEventStatus status,
            Pageable pageable);

    /**
     * Find failed events that can be retried
     */
    @Query("""
            SELECT e FROM OutboxEvent e
            WHERE e.status = :status
            AND e.retryCount < :maxRetries
            AND (e.lastRetryAt IS NULL OR e.lastRetryAt < :retryThreshold)
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEvent> findRetryableEvents(
            @Param("status") OutboxEventStatus status,
            @Param("maxRetries") int maxRetries,
            @Param("retryThreshold") LocalDateTime retryThreshold,
            Pageable pageable);

    /**
     * Find events by aggregate
     */
    List<OutboxEvent> findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
            String aggregateType,
            String aggregateId);

    /**
     * Find events for cleanup
     */
    @Query("""
            SELECT e FROM OutboxEvent e
            WHERE e.status = :status
            AND e.publishedAt < :cleanupThreshold
            """)
    List<OutboxEvent> findEventsForCleanup(
            @Param("status") OutboxEventStatus status,
            @Param("cleanupThreshold") LocalDateTime cleanupThreshold);

    /**
     * Count events by status
     */
    long countByStatus(OutboxEventStatus status);

    /**
     * Count events by status and time range
     */
    @Query("""
            SELECT COUNT(e) FROM OutboxEvent e
            WHERE e.status = :status
            AND e.createdAt BETWEEN :startTime AND :endTime
            """)
    long countByStatusAndTimeRange(
            @Param("status") OutboxEventStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Bulk delete old published events
     */
    @Modifying
    @Query("""
            DELETE FROM OutboxEvent e
            WHERE e.status = :status
            AND e.publishedAt < :cleanupThreshold
            """)
    int deleteOldPublishedEvents(
            @Param("status") OutboxEventStatus status,
            @Param("cleanupThreshold") LocalDateTime cleanupThreshold);

    /**
     * Find events by correlation ID for tracing
     */
    @Query("""
            SELECT e FROM OutboxEvent e
            WHERE e.eventMetadata LIKE %:correlationId%
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEvent> findByCorrelationId(@Param("correlationId") String correlationId);

    /**
     * Find events that might be stuck
     */
    @Query("""
            SELECT e FROM OutboxEvent e
            WHERE e.status = :status
            AND e.createdAt < :stuckThreshold
            """)
    List<OutboxEvent> findStuckEvents(
            @Param("status") OutboxEventStatus status,
            @Param("stuckThreshold") LocalDateTime stuckThreshold);
}