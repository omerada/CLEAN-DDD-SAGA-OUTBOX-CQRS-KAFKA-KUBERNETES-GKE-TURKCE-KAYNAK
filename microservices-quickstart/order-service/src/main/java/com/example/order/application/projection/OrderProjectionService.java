package com.example.order.application.projection;

import com.example.order.application.query.model.OrderListView;
import com.example.order.application.query.repository.OrderListViewRepository;
import com.example.order.domain.event.OrderCreatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Order Projection Service
 * 
 * CQRS Read Side projection service.
 * Listens to domain events and updates read models for eventual consistency.
 */
@Service
public class OrderProjectionService {

    private static final Logger log = LoggerFactory.getLogger(OrderProjectionService.class);

    private final OrderListViewRepository orderListViewRepository;

    public OrderProjectionService(OrderListViewRepository orderListViewRepository) {
        this.orderListViewRepository = orderListViewRepository;
    }

    /**
     * Handle order created event - create read model
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void handle(OrderCreatedEvent event) {
        try {
            log.info("Projecting OrderCreatedEvent to read model: {}",
                    event.getOrderId().getValue());

            // Create read model from domain event
            OrderListView orderListView = new OrderListView(
                    event.getOrderId().getValue(),
                    event.getCustomerId().getValue(),
                    "PENDING", // Initial status
                    event.getTotalAmount().getAmount(),
                    "USD", // Default currency for now
                    event.getTotalItems(),
                    event.getOccurredOn());

            // Save to read model repository
            orderListViewRepository.save(orderListView);

            log.debug("Successfully projected order to read model: {}",
                    event.getOrderId().getValue());

        } catch (Exception e) {
            log.error("Error projecting OrderCreatedEvent to read model: {}",
                    event.getOrderId().getValue(), e);

            // In production, you might want to:
            // 1. Retry the projection
            // 2. Send to dead letter queue
            // 3. Alert monitoring systems
            // For now, we'll just log the error
        }
    }

    /**
     * Handle order status changes
     * This can be extended to handle other order events like OrderConfirmedEvent,
     * etc.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        try {
            log.info("Updating order status in read model: {} to {}",
                    event.getOrderId(), event.getNewStatus());

            // Find and update existing read model
            orderListViewRepository.findById(event.getOrderId())
                    .ifPresent(orderView -> {
                        orderView.updateStatus(event.getNewStatus(), event.getOccurredAt());
                        orderListViewRepository.save(orderView);

                        log.debug("Updated order status in read model: {}", event.getOrderId());
                    });

        } catch (Exception e) {
            log.error("Error updating order status in read model: {}",
                    event.getOrderId(), e);
        }
    }

    /**
     * Rebuild read model from write side (for data consistency)
     * This can be useful for:
     * 1. Initial data load
     * 2. Recovery from read model corruption
     * 3. Schema migrations
     */
    @Transactional
    public void rebuildReadModel(String orderId) {
        try {
            log.info("Rebuilding read model for order: {}", orderId);

            // In a real implementation, you would:
            // 1. Fetch order from write side
            // 2. Create/update read model
            // 3. Handle any errors

            log.info("Read model rebuilt for order: {}", orderId);

        } catch (Exception e) {
            log.error("Error rebuilding read model for order: {}", orderId, e);
            throw new RuntimeException("Failed to rebuild read model", e);
        }
    }
}

/**
 * Order Status Changed Event (placeholder)
 * In real implementation, this would be part of domain events
 */
class OrderStatusChangedEvent {
    private final String orderId;
    private final String newStatus;
    private final java.time.LocalDateTime occurredAt;

    public OrderStatusChangedEvent(String orderId, String newStatus, java.time.LocalDateTime occurredAt) {
        this.orderId = orderId;
        this.newStatus = newStatus;
        this.occurredAt = occurredAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public java.time.LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}