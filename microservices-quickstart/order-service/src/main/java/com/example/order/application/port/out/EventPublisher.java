package com.example.order.application.port.out;

import com.example.order.domain.event.*;

/**
 * Event Publisher Port (Secondary Port)
 * 
 * Abstraction for publishing domain events
 * Technology-agnostic interface for event publication
 */
public interface EventPublisher {

    /**
     * Publish order created event
     */
    void publish(OrderCreatedEvent event);

    /**
     * Publish order confirmed event
     */
    void publish(OrderConfirmedEvent event);

    /**
     * Publish order cancelled event
     */
    void publish(OrderCancelledEvent event);

    /**
     * Publish generic domain event
     * Uses pattern matching or reflection to determine event type
     */
    void publishDomainEvent(Object event);

    /**
     * Publish multiple events in batch
     * Useful for transactional event publishing
     */
    void publishAll(java.util.List<Object> events);

    /**
     * Check if publisher is available
     */
    boolean isAvailable();
}