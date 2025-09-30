package com.example.order.domain.outbox;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event Metadata for tracing and correlation
 * 
 * Contains tracking information for events across distributed systems
 */
public record EventMetadata(
        String correlationId,
        String causationId,
        String userId,
        String traceId,
        LocalDateTime timestamp,
        String source,
        int version) {

    /**
     * Create event metadata with correlation ID and source
     */
    public static EventMetadata create(String correlationId, String source) {
        return new EventMetadata(
                correlationId,
                null, // causationId
                null, // userId - will be set by application service
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                source,
                1);
    }

    /**
     * Create event metadata with full context
     */
    public static EventMetadata create(
            String correlationId,
            String causationId,
            String userId,
            String source) {
        return new EventMetadata(
                correlationId,
                causationId,
                userId,
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                source,
                1);
    }

    /**
     * Create child event metadata (causation chain)
     */
    public EventMetadata createChild(String newCorrelationId, String source) {
        return new EventMetadata(
                newCorrelationId,
                this.correlationId, // Parent becomes causation
                this.userId,
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                source,
                this.version + 1);
    }
}