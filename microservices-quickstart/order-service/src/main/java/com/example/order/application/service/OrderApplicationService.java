package com.example.order.application.service;

import com.example.order.application.port.in.*;
import com.example.order.application.port.out.*;
import com.example.order.domain.entity.Order;
import com.example.order.domain.entity.OrderItem;
import com.example.order.domain.outbox.OutboxEvent;
import com.example.order.domain.outbox.EventMetadata;
import com.example.order.domain.event.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Order Application Service
 * 
 * Orchestrates business use cases by:
 * - Coordinating between domain and infrastructure
 * - Managing transactions
 * - Publishing domain events via Outbox Pattern
 * - Handling cross-cutting concerns
 */
@Service
@Transactional
public class OrderApplicationService implements
        CreateOrderUseCase,
        GetOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(OrderApplicationService.class);

    private final OrderRepositoryPort orderRepository;
    private final OutboxRepositoryPort outboxRepository;

    public OrderApplicationService(
            OrderRepositoryPort orderRepository,
            OutboxRepositoryPort outboxRepository) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
    }

    @Override
    public CreateOrderResponse createOrder(CreateOrderCommand command) {
        try {
            log.info("Creating order for customer: {}", command.customerId().getValue());

            // 1. Convert command to domain objects
            List<OrderItem> orderItems = command.items().stream()
                    .map(this::toDomainOrderItem)
                    .toList();

            // 2. Create domain aggregate using business logic
            Order order = Order.create(command.customerId(), orderItems);

            // 3. Persist aggregate - ATOMIC TRANSACTION START
            orderRepository.save(order);

            // 4. Create outbox events - SAME TRANSACTION
            createOutboxEventsForOrderCreation(order);

            log.info("Order created successfully with outbox events: {}", order.getId().getValue());

            // 5. Return response
            return CreateOrderResponse.from(order);

        } catch (Exception e) {
            log.error("Failed to create order for customer: {}", command.customerId().getValue(), e);
            throw new RuntimeException("Order creation failed", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GetOrderResponse getOrder(GetOrderQuery query) {
        Order order = orderRepository.findById(query.orderId())
                .orElse(null);

        if (order == null) {
            return null; // Let controller handle 404
        }

        return GetOrderResponse.from(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GetOrderResponse> getOrdersByCustomer(GetOrdersByCustomerQuery query) {
        List<Order> orders = orderRepository.findByCustomerId(query.customerId());

        return orders.stream()
                .map(GetOrderResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GetOrderResponse> getAllOrders(GetAllOrdersQuery query) {
        List<Order> orders;

        if (query.statusFilter() != null) {
            orders = orderRepository.findByStatus(query.statusFilter());
        } else {
            // For now, just get all orders - pagination can be added later
            orders = orderRepository.findAll();
        }

        return orders.stream()
                .map(GetOrderResponse::from)
                .toList();
    }

    /**
     * Convert command item to domain entity
     */
    private OrderItem toDomainOrderItem(OrderItemCommand itemCommand) {
        return OrderItem.create(
                itemCommand.productId(),
                itemCommand.quantity(),
                itemCommand.unitPrice());
    }

    /**
     * Create outbox events for order creation
     */
    private void createOutboxEventsForOrderCreation(Order order) {
        // Create correlation ID for event tracking
        String correlationId = UUID.randomUUID().toString();
        EventMetadata metadata = EventMetadata.create(correlationId, "order-service");

        // Create Order Created Event
        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getItems().size(),
                LocalDateTime.now());

        OutboxEvent orderCreatedOutboxEvent = OutboxEvent.create(
                "Order",
                order.getId().getValue(),
                "OrderCreated",
                orderCreatedEvent,
                metadata);

        outboxRepository.save(orderCreatedOutboxEvent);

        log.debug("Created outbox event for OrderCreated: {}", order.getId().getValue());
    }
}