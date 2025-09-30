package com.example.order.infrastructure.adapter.out.persistence;

import com.example.order.domain.entity.Order;
import com.example.order.domain.entity.OrderItem;
import com.example.order.domain.valueobject.*;
import com.example.order.infrastructure.adapter.out.persistence.entity.OrderJpaEntity;
import com.example.order.infrastructure.adapter.out.persistence.entity.OrderItemJpaEntity;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Domain-Infrastructure Mapper
 * 
 * Converts between domain entities and JPA entities
 * Maintains Clean Architecture boundaries by isolating mapping logic
 */
@Component
public class OrderPersistenceMapper {

    /**
     * Convert domain Order to JPA entity
     */
    public OrderJpaEntity toJpaEntity(Order order) {
        OrderJpaEntity.OrderStatusEnum jpaStatus = mapStatusToJpa(order.getStatus());
        
        OrderJpaEntity jpaEntity = new OrderJpaEntity(
            order.getId().getValue(),
            order.getCustomerId().getValue(),
            jpaStatus,
            order.getTotalAmount().getAmount(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );

        List<OrderItemJpaEntity> jpaItems = order.getItems().stream()
            .map(this::toJpaEntity)
            .collect(Collectors.toList());
            
        jpaEntity.setItems(jpaItems);
        
        return jpaEntity;
    }

    /**
     * Convert domain OrderItem to JPA entity
     */
    private OrderItemJpaEntity toJpaEntity(OrderItem orderItem) {
        return new OrderItemJpaEntity(
            orderItem.getProductId().getValue(),
            orderItem.getQuantity(),
            orderItem.getUnitPrice().getAmount(),
            orderItem.getSubtotal().getAmount()
        );
    }

    /**
     * Convert JPA entity to domain Order
     */
    public Order toDomainEntity(OrderJpaEntity jpaEntity) {
        CustomerId customerId = CustomerId.of(jpaEntity.getCustomerId());

        List<OrderItem> domainItems = jpaEntity.getItems().stream()
            .map(this::toDomainEntity)
            .collect(Collectors.toList());

        // Create order through static factory method  
        Order order = Order.create(customerId, domainItems);
        
        // For persistence reconstitution, we need to use reflection or add reconstitution methods
        // For now, let's create a simple approach using the existing constructor
        try {
            java.lang.reflect.Field idField = Order.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(order, OrderId.of(jpaEntity.getOrderId()));
            
            java.lang.reflect.Field createdAtField = Order.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(order, jpaEntity.getCreatedAt());
            
            java.lang.reflect.Field updatedAtField = Order.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(order, jpaEntity.getUpdatedAt());
            
            // Handle status transitions
            OrderStatus targetStatus = mapStatusToDomain(jpaEntity.getStatus());
            if (targetStatus == OrderStatus.CONFIRMED) {
                order.confirm();
            } else if (targetStatus == OrderStatus.CANCELLED) {
                order.cancel("Restored from persistence");
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to reconstitute Order from persistence", e);
        }

        return order;
    }

    /**
     * Convert JPA OrderItem to domain entity
     */
    private OrderItem toDomainEntity(OrderItemJpaEntity jpaEntity) {
        ProductId productId = ProductId.of(jpaEntity.getProductId());
        Money unitPrice = Money.of(jpaEntity.getUnitPrice());
        
        return OrderItem.create(productId, jpaEntity.getQuantity(), unitPrice);
    }

    /**
     * Map domain status to JPA enum
     */
    private OrderJpaEntity.OrderStatusEnum mapStatusToJpa(OrderStatus domainStatus) {
        return switch (domainStatus) {
            case PENDING -> OrderJpaEntity.OrderStatusEnum.PENDING;
            case CONFIRMED -> OrderJpaEntity.OrderStatusEnum.CONFIRMED;
            case CANCELLED -> OrderJpaEntity.OrderStatusEnum.CANCELLED;
            case SHIPPED, DELIVERED, FAILED -> 
                throw new IllegalArgumentException("Status not supported in persistence: " + domainStatus);
        };
    }

    /**
     * Map JPA enum to domain status
     */
    private OrderStatus mapStatusToDomain(OrderJpaEntity.OrderStatusEnum jpaStatus) {
        return switch (jpaStatus) {
            case PENDING -> OrderStatus.PENDING;
            case CONFIRMED -> OrderStatus.CONFIRMED;
            case CANCELLED -> OrderStatus.CANCELLED;
        };
    }
}