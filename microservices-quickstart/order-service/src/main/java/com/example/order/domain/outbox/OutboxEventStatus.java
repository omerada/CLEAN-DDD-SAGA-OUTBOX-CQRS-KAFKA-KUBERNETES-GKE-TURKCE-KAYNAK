package com.example.order.domain.outbox;

/**
 * Outbox Event Status
 * 
 * Represents the lifecycle states of outbox events
 */
public enum OutboxEventStatus {
    PENDING, // Event created, waiting to be published
    PUBLISHED, // Event successfully published to message broker
    FAILED // Event publishing failed, may be retried
}