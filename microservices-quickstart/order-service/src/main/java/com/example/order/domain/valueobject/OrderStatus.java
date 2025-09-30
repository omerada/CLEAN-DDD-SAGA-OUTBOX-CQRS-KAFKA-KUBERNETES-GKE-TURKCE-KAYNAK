package com.example.order.domain.valueobject;

/**
 * Order Status Value Object
 * 
 * Represents valid states in the order lifecycle
 * Enforces valid state transitions through business rules
 */
public enum OrderStatus {
    PENDING("Order created, waiting for payment"),
    CONFIRMED("Payment confirmed, processing"),
    SHIPPED("Order shipped to customer"),
    DELIVERED("Order delivered successfully"),
    CANCELLED("Order cancelled"),
    FAILED("Order processing failed");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Business rules for valid status transitions
     */
    public boolean canTransitionTo(OrderStatus newStatus) {
        return switch (this) {
            case PENDING -> newStatus == CONFIRMED || newStatus == CANCELLED || newStatus == FAILED;
            case CONFIRMED -> newStatus == SHIPPED || newStatus == CANCELLED;
            case SHIPPED -> newStatus == DELIVERED;
            case DELIVERED, CANCELLED, FAILED -> false; // Terminal states
        };
    }

    /**
     * Check if this is a terminal state
     */
    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED || this == FAILED;
    }

    /**
     * Check if order can be modified
     */
    public boolean isModifiable() {
        return this == PENDING;
    }

    /**
     * Check if order can be cancelled
     */
    public boolean isCancellable() {
        return this == PENDING || this == CONFIRMED;
    }
}