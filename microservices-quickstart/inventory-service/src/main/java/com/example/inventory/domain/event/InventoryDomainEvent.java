package com.example.inventory.domain.event;

import java.time.LocalDateTime;

/**
 * Base abstract class for all inventory domain events
 * Extends the base DomainEvent class
 */
public abstract class InventoryDomainEvent extends DomainEvent {

    private final String aggregateId;

    protected InventoryDomainEvent(String eventType, String aggregateId) {
        super(eventType);
        this.aggregateId = aggregateId;
    }

    protected InventoryDomainEvent(String eventId, LocalDateTime occurredOn,
            String eventType, String aggregateId) {
        super(eventId, occurredOn, eventType);
        this.aggregateId = aggregateId;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public LocalDateTime getOccurredAt() {
        return getOccurredOn();
    }
}