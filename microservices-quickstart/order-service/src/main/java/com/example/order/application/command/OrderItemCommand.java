package com.example.order.application.command;

import com.example.order.domain.entity.OrderItem;
import com.example.order.domain.valueobject.ProductId;
import com.example.order.domain.valueobject.Money;
import com.example.order.domain.valueobject.Quantity;

import java.math.BigDecimal;

/**
 * Order Item Command
 * 
 * CQRS Command object for order item data
 */
public record OrderItemCommand(
    String productId,
    Integer quantity,
    BigDecimal unitPrice
) {
    
    public OrderItemCommand {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Product ID is required");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit price must be positive");
        }
    }
    
    /**
     * Convert to domain entity
     */
    public OrderItem toDomainEntity() {
        return OrderItem.create(
            ProductId.of(productId),
            Quantity.of(quantity),
            Money.of(unitPrice)
        );
    }
}