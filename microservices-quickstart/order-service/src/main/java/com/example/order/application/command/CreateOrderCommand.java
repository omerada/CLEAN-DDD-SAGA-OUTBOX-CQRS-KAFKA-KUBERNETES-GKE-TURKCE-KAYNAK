package com.example.order.application.command;

import com.example.order.domain.valueobject.CustomerId;
import com.example.order.domain.entity.OrderItem;

import java.util.List;

/**
 * Create Order Command
 * 
 * CQRS Write Side Command for creating new orders.
 * Contains all necessary data for order creation business logic.
 */
public record CreateOrderCommand(
    CustomerId customerId,
    List<OrderItemCommand> items
) {
    
    public CreateOrderCommand {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order items are required");
        }
    }
    
    /**
     * Convert command items to domain entities
     */
    public List<OrderItem> toDomainItems() {
        return items.stream()
                .map(OrderItemCommand::toDomainEntity)
                .toList();
    }
}