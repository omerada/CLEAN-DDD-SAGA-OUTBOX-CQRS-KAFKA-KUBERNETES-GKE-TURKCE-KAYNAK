package com.example.inventory.infrastructure.event;

import com.example.inventory.application.port.out.EventPublisher;
import com.example.inventory.domain.event.DomainEvent;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Kafka-based Event Publisher implementation
 * 
 * This component implements the EventPublisher port using Kafka
 * for publishing domain events to other services.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String INVENTORY_TOPIC = "inventory-events";

    @Override
    public void publishEvent(DomainEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(INVENTORY_TOPIC, event.getEventId(), eventJson)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Successfully published event: {} to topic: {}",
                                    event.getEventId(), INVENTORY_TOPIC);
                        } else {
                            log.error("Failed to publish event: {} to topic: {}",
                                    event.getEventId(), INVENTORY_TOPIC, ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Error serializing event: {}", event.getEventId(), e);
        }
    }

    @Override
    public void publishEventAsync(DomainEvent event) {
        // For simplicity, using the same implementation
        // In production, you might want different configuration for async
        publishEvent(event);
    }
}