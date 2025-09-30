package com.example.order.domain.event;

import com.example.order.domain.valueobject.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Order Created Domain Event
 * 
 * Published when a new order is successfully created
 * Used for event-driven communication with other bounded contexts
 */
public class OrderCreatedEvent {
    private final OrderId orderId;
    private final CustomerId customerId;
    private final Money totalAmount;
    private final LocalDateTime occurredOn;
    private final int totalItems;

    public OrderCreatedEvent(OrderId orderId, CustomerId customerId, 
                           Money totalAmount, int totalItems, LocalDateTime occurredOn) {
        this.orderId = Objects.requireNonNull(orderId, "Order ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.totalAmount = Objects.requireNonNull(totalAmount, "Total amount cannot be null");
        this.totalItems = totalItems;
        this.occurredOn = Objects.requireNonNull(occurredOn, "Occurred on cannot be null");
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderCreatedEvent that = (OrderCreatedEvent) o;
        return totalItems == that.totalItems &&
               Objects.equals(orderId, that.orderId) &&
               Objects.equals(customerId, that.customerId) &&
               Objects.equals(totalAmount, that.totalAmount) &&
               Objects.equals(occurredOn, that.occurredOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, customerId, totalAmount, occurredOn, totalItems);
    }

    @Override
    public String toString() {
        return "OrderCreatedEvent{" +
                "orderId=" + orderId +
                ", customerId=" + customerId +
                ", totalAmount=" + totalAmount +
                ", totalItems=" + totalItems +
                ", occurredOn=" + occurredOn +
                '}';
    }
}