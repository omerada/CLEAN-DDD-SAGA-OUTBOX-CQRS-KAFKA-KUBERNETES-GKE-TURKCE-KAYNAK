package com.example.order.application.command.handler;

import com.example.order.application.command.CreateOrderCommand;
import com.example.order.application.port.out.OrderRepositoryPort;
import com.example.order.application.port.out.OutboxRepositoryPort;
import com.example.order.domain.entity.Order;
import com.example.order.domain.outbox.OutboxEvent;
import com.example.order.domain.outbox.EventMetadata;
import com.example.order.domain.event.OrderCreatedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Order Command Handler
 * 
 * CQRS Write Side command handler for order operations.
 * Handles business logic and command processing.
 */
@Component
public class OrderCommandHandler {
    
    private static final Logger log = LoggerFactory.getLogger(OrderCommandHandler.class);
    
    private final OrderRepositoryPort orderRepository;
    private final OutboxRepositoryPort outboxRepository;
    
    public OrderCommandHandler(
            OrderRepositoryPort orderRepository,
            OutboxRepositoryPort outboxRepository) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
    }
    
    /**
     * Handle create order command
     */
    @Transactional
    public CreateOrderResult handle(CreateOrderCommand command) {
        try {
            log.info("Processing create order command for customer: {}", 
                    command.customerId().getValue());
            
            // 1. Create domain aggregate with business logic
            Order order = Order.create(
                command.customerId(),
                command.toDomainItems()
            );
            
            // 2. Persist aggregate - WRITE SIDE
            Order savedOrder = orderRepository.save(order);
            
            // 3. Create outbox events for read side updates
            createOutboxEventsForOrderCreation(savedOrder);
            
            log.info("Order created successfully: {}", savedOrder.getId().getValue());
            
            return CreateOrderResult.success(savedOrder.getId());
            
        } catch (Exception e) {
            log.error("Failed to process create order command for customer: {}", 
                     command.customerId().getValue(), e);
            
            return CreateOrderResult.failure(e.getMessage());
        }
    }
    
    /**
     * Create outbox events for order creation
     * These events will trigger read model updates
     */
    private void createOutboxEventsForOrderCreation(Order order) {
        // Create correlation ID for event tracking
        String correlationId = UUID.randomUUID().toString();
        EventMetadata metadata = EventMetadata.create(correlationId, "order-command-handler");
        
        // Create Order Created Event for read side projection
        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(
            order.getId(),
            order.getCustomerId(),
            order.getTotalAmount(),
            order.getItems().size(),
            LocalDateTime.now()
        );
        
        OutboxEvent orderCreatedOutboxEvent = OutboxEvent.create(
            "Order",
            order.getId().getValue(),
            "OrderCreated",
            orderCreatedEvent,
            metadata
        );
        
        outboxRepository.save(orderCreatedOutboxEvent);
        
        log.debug("Created outbox event for read side projection: {}", 
                 order.getId().getValue());
    }
}