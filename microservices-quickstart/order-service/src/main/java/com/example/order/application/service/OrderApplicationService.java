package com.example.order.application.service;

import com.example.order.application.port.in.*;
import com.example.order.application.port.out.*;
import com.example.order.domain.entity.Order;
import com.example.order.domain.entity.OrderItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Order Application Service
 * 
 * Orchestrates business use cases by:
 * - Coordinating between domain and infrastructure
 * - Managing transactions
 * - Publishing domain events
 * - Handling cross-cutting concerns
 */
@Service
@Transactional
public class OrderApplicationService implements
        CreateOrderUseCase,
        GetOrderUseCase {

    private final OrderRepositoryPort orderRepository;
    // private final EventPublisher eventPublisher; // TODO: Implement later

    public OrderApplicationService(OrderRepositoryPort orderRepository) {
        this.orderRepository = orderRepository;
        // this.eventPublisher = eventPublisher; // TODO: Implement later
    }

    @Override
    public CreateOrderResponse createOrder(CreateOrderCommand command) {
        // 1. Convert command to domain objects
        List<OrderItem> orderItems = command.items().stream()
                .map(this::toDomainOrderItem)
                .toList();

        // 2. Create domain aggregate using business logic
        Order order = Order.create(command.customerId(), orderItems);

        // 3. Persist aggregate
        orderRepository.save(order);

        // 4. TODO: Publish domain events (outbox pattern can be applied here)
        // publishDomainEvents(order);

        // 5. TODO: Clear events to prevent re-publishing
        // order.clearDomainEvents();

        // 6. Return response
        return CreateOrderResponse.from(order);
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

    // TODO: Implement event publishing
    // private void publishDomainEvents(Order order) {
    // List<Object> domainEvents = order.getDomainEvents();
    // domainEvents.forEach(eventPublisher::publishDomainEvent);
    // }
}