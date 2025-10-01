package com.example.inventory.application.port.out;

import com.example.inventory.domain.event.DomainEvent;

/**
 * Output port for publishing domain events
 * 
 * This interface defines the contract for event publishing
 * following the Hexagonal Architecture principles.
 */
public interface EventPublisher {

    /**
     * Publish a domain event
     * 
     * @param event the domain event to publish
     */
    void publishEvent(DomainEvent event);

    /**
     * Publish a domain event asynchronously
     * 
     * @param event the domain event to publish
     */
    void publishEventAsync(DomainEvent event);
}