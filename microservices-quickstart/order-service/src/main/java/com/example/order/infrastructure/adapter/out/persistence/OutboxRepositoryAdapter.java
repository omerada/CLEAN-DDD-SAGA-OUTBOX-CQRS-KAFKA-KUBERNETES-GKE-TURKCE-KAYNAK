package com.example.order.infrastructure.adapter.out.persistence;

import com.example.order.application.port.out.OutboxRepositoryPort;
import com.example.order.domain.outbox.OutboxEvent;
import com.example.order.domain.outbox.OutboxEventStatus;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Outbox Repository Adapter
 * 
 * Implements OutboxRepositoryPort using JPA technology
 */
@Component
@Transactional
public class OutboxRepositoryAdapter implements OutboxRepositoryPort {

    private final OutboxEventJpaRepository jpaRepository;

    public OutboxRepositoryAdapter(OutboxEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public OutboxEvent save(OutboxEvent outboxEvent) {
        return jpaRepository.save(outboxEvent);
    }

    @Override
    public List<OutboxEvent> saveAll(List<OutboxEvent> outboxEvents) {
        return jpaRepository.saveAll(outboxEvents);
    }

    @Override
    public Optional<OutboxEvent> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findPendingEvents(Pageable pageable) {
        return jpaRepository.findPendingEvents(OutboxEventStatus.PENDING, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findRetryableEvents(
            int maxRetries,
            LocalDateTime retryThreshold,
            Pageable pageable) {
        return jpaRepository.findRetryableEvents(
                OutboxEventStatus.FAILED,
                maxRetries,
                retryThreshold,
                pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findByAggregateTypeAndAggregateId(
            String aggregateType,
            String aggregateId) {
        return jpaRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                aggregateType,
                aggregateId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findEventsForCleanup(LocalDateTime cleanupThreshold) {
        return jpaRepository.findEventsForCleanup(
                OutboxEventStatus.PUBLISHED,
                cleanupThreshold);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(OutboxEventStatus status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatusAndTimeRange(
            OutboxEventStatus status,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        return jpaRepository.countByStatusAndTimeRange(status, startTime, endTime);
    }

    @Override
    public int deleteOldPublishedEvents(LocalDateTime cleanupThreshold) {
        return jpaRepository.deleteOldPublishedEvents(
                OutboxEventStatus.PUBLISHED,
                cleanupThreshold);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findByCorrelationId(String correlationId) {
        return jpaRepository.findByCorrelationId(correlationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findStuckEvents(LocalDateTime stuckThreshold) {
        return jpaRepository.findStuckEvents(OutboxEventStatus.PENDING, stuckThreshold);
    }
}