package com.example.order.domain.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbox Event Entity
 * 
 * Stores domain events that need to be published to external systems.
 * Ensures atomicity between business operations and event publishing
 * through database transactions.
 */
@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_status_created", columnList = "status, createdAt"),
        @Index(name = "idx_outbox_aggregate_id", columnList = "aggregateId"),
        @Index(name = "idx_outbox_event_type", columnList = "eventType")
})
public class OutboxEvent {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Lob
    @Column(name = "event_data", nullable = false)
    private String eventData;

    @Lob
    @Column(name = "event_metadata")
    private String eventMetadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "retry_count")
    private int retryCount = 0;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Version
    @Column(name = "version")
    private Long version;

    // JPA constructor
    protected OutboxEvent() {
    }

    private OutboxEvent(
            String id,
            String aggregateType,
            String aggregateId,
            String eventType,
            String eventData,
            String eventMetadata,
            OutboxEventStatus status,
            LocalDateTime createdAt) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventData = eventData;
        this.eventMetadata = eventMetadata;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * Create new outbox event from domain event
     */
    public static OutboxEvent create(
            String aggregateType,
            String aggregateId,
            String eventType,
            Object eventPayload,
            EventMetadata metadata) {
        try {
            ObjectMapper mapper = createObjectMapper();

            String eventData = mapper.writeValueAsString(eventPayload);
            String eventMetadata = metadata != null ? mapper.writeValueAsString(metadata) : null;

            return new OutboxEvent(
                    UUID.randomUUID().toString(),
                    aggregateType,
                    aggregateId,
                    eventType,
                    eventData,
                    eventMetadata,
                    OutboxEventStatus.PENDING,
                    LocalDateTime.now());

        } catch (Exception e) {
            throw new OutboxEventCreationException("Failed to create outbox event", e);
        }
    }

    /**
     * Mark event as successfully published
     */
    public void markAsPublished() {
        if (this.status != OutboxEventStatus.PENDING) {
            throw new IllegalOutboxEventStateException(
                    "Cannot mark event as published. Current status: " + this.status);
        }

        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    /**
     * Mark event as failed with error message
     */
    public void markAsFailed(String errorMessage) {
        this.status = OutboxEventStatus.FAILED;
        this.errorMessage = errorMessage != null && errorMessage.length() > 1000 ? errorMessage.substring(0, 1000)
                : errorMessage;
        this.lastRetryAt = LocalDateTime.now();
        this.retryCount++;
    }

    /**
     * Reset event for retry attempt
     */
    public void resetForRetry() {
        if (this.status != OutboxEventStatus.FAILED) {
            throw new IllegalOutboxEventStateException(
                    "Cannot retry event. Current status: " + this.status);
        }

        this.status = OutboxEventStatus.PENDING;
        this.errorMessage = null;
    }

    /**
     * Check if event can be retried
     */
    public boolean canRetry(int maxRetries) {
        return this.status == OutboxEventStatus.FAILED &&
                this.retryCount < maxRetries;
    }

    /**
     * Check if event is old enough for cleanup
     */
    public boolean canBeCleanedUp(LocalDateTime cleanupThreshold) {
        return this.status == OutboxEventStatus.PUBLISHED &&
                this.publishedAt != null &&
                this.publishedAt.isBefore(cleanupThreshold);
    }

    /**
     * Deserialize event data to specific type
     */
    public <T> T getEventPayload(Class<T> eventClass) {
        try {
            ObjectMapper mapper = createObjectMapper();
            return mapper.readValue(this.eventData, eventClass);
        } catch (Exception e) {
            throw new OutboxEventDeserializationException(
                    "Failed to deserialize event data", e);
        }
    }

    /**
     * Deserialize event metadata
     */
    public EventMetadata getDeserializedMetadata() {
        if (this.eventMetadata == null) {
            return null;
        }

        try {
            ObjectMapper mapper = createObjectMapper();
            return mapper.readValue(this.eventMetadata, EventMetadata.class);
        } catch (Exception e) {
            throw new OutboxEventDeserializationException(
                    "Failed to deserialize event metadata", e);
        }
    }

    /**
     * Create configured ObjectMapper
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventData() {
        return eventData;
    }

    public String getEventMetadata() {
        return eventMetadata;
    }

    public OutboxEventStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public LocalDateTime getLastRetryAt() {
        return lastRetryAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "OutboxEvent{" +
                "id='" + id + '\'' +
                ", aggregateType='" + aggregateType + '\'' +
                ", aggregateId='" + aggregateId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", retryCount=" + retryCount +
                '}';
    }
}

/**
 * Exception thrown when outbox event creation fails
 */
class OutboxEventCreationException extends RuntimeException {
    public OutboxEventCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Exception thrown when outbox event state transition is invalid
 */
class IllegalOutboxEventStateException extends RuntimeException {
    public IllegalOutboxEventStateException(String message) {
        super(message);
    }
}

/**
 * Exception thrown when outbox event deserialization fails
 */
class OutboxEventDeserializationException extends RuntimeException {
    public OutboxEventDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}