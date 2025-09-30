package com.example.order.domain.entity;

import com.example.order.domain.valueobject.*;
import java.util.Objects;

/**
 * Order Item Entity
 * 
 * Represents a single item within an order
 * Contains business rules for:
 * - Quantity validation
 * - Price calculation
 * - Subtotal computation
 */
public class OrderItem {
    private ProductId productId;
    private int quantity;
    private Money unitPrice;
    private Money subtotal;

    // Private constructor for framework/persistence
    private OrderItem() {}

    private OrderItem(ProductId productId, int quantity, Money unitPrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = calculateSubtotal();
    }

    /**
     * Factory method with business rule validation
     */
    public static OrderItem create(ProductId productId, int quantity, Money unitPrice) {
        validateInputs(productId, quantity, unitPrice);
        return new OrderItem(productId, quantity, unitPrice);
    }

    private static void validateInputs(ProductId productId, int quantity, Money unitPrice) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (quantity > 1000) {
            throw new IllegalArgumentException("Quantity cannot exceed 1000 per item");
        }
        if (unitPrice == null || !unitPrice.isPositive()) {
            throw new IllegalArgumentException("Unit price must be positive");
        }
    }

    /**
     * Business logic: Update quantity with validation
     */
    public void updateQuantity(int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (newQuantity > 1000) {
            throw new IllegalArgumentException("Quantity cannot exceed 1000 per item");
        }
        this.quantity = newQuantity;
        this.subtotal = calculateSubtotal();
    }

    /**
     * Business logic: Update unit price with recalculation
     */
    public void updateUnitPrice(Money newUnitPrice) {
        if (newUnitPrice == null || !newUnitPrice.isPositive()) {
            throw new IllegalArgumentException("Unit price must be positive");
        }
        this.unitPrice = newUnitPrice;
        this.subtotal = calculateSubtotal();
    }

    private Money calculateSubtotal() {
        return unitPrice.multiply(quantity);
    }

    // Getters
    public ProductId getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public Money getSubtotal() {
        return subtotal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return quantity == orderItem.quantity &&
               Objects.equals(productId, orderItem.productId) &&
               Objects.equals(unitPrice, orderItem.unitPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, quantity, unitPrice);
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "productId=" + productId +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", subtotal=" + subtotal +
                '}';
    }
}