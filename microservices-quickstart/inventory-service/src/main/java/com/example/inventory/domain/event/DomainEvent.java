package com.example.inventory.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all domain events
 * 
 * This abstract class provides common properties and behavior
 * for domain events in the inventory service.
 */
public abstract class DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;
    private final String eventType;

    protected DomainEvent(String eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.eventType = eventType;
    }

    protected DomainEvent(String eventId, LocalDateTime occurredOn, String eventType) {
        this.eventId = eventId;
        this.occurredOn = occurredOn;
        this.eventType = eventType;
    }

    public String getEventId() {
        return eventId;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    public String getEventType() {
        return eventType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        DomainEvent that = (DomainEvent) obj;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s{eventId='%s', occurredOn=%s}",
                getClass().getSimpleName(), eventId, occurredOn);
    }
}