package com.example.order.domain.event;

import com.example.order.domain.valueobject.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Order Cancelled Domain Event
 * 
 * Published when an order is cancelled
 * Contains cancellation reason for audit purposes
 */
public class OrderCancelledEvent {
    private final OrderId orderId;
    private final CustomerId customerId;
    private final String reason;
    private final LocalDateTime occurredOn;

    public OrderCancelledEvent(OrderId orderId, CustomerId customerId, 
                             String reason, LocalDateTime occurredOn) {
        this.orderId = Objects.requireNonNull(orderId, "Order ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.reason = reason != null ? reason : "No reason provided";
        this.occurredOn = Objects.requireNonNull(occurredOn, "Occurred on cannot be null");
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderCancelledEvent that = (OrderCancelledEvent) o;
        return Objects.equals(orderId, that.orderId) &&
               Objects.equals(customerId, that.customerId) &&
               Objects.equals(reason, that.reason) &&
               Objects.equals(occurredOn, that.occurredOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, customerId, reason, occurredOn);
    }

    @Override
    public String toString() {
        return "OrderCancelledEvent{" +
                "orderId=" + orderId +
                ", customerId=" + customerId +
                ", reason='" + reason + '\'' +
                ", occurredOn=" + occurredOn +
                '}';
    }
}