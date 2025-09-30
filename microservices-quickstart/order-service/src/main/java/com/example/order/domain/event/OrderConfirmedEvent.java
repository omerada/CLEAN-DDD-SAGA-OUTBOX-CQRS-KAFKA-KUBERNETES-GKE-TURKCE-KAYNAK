package com.example.order.domain.event;

import com.example.order.domain.valueobject.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Order Confirmed Domain Event
 * 
 * Published when an order moves from PENDING to CONFIRMED status
 * Usually triggered after successful payment
 */
public class OrderConfirmedEvent {
    private final OrderId orderId;
    private final CustomerId customerId;
    private final Money totalAmount;
    private final LocalDateTime occurredOn;

    public OrderConfirmedEvent(OrderId orderId, CustomerId customerId,
            Money totalAmount, LocalDateTime occurredOn) {
        this.orderId = Objects.requireNonNull(orderId, "Order ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.totalAmount = Objects.requireNonNull(totalAmount, "Total amount cannot be null");
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

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OrderConfirmedEvent that = (OrderConfirmedEvent) o;
        return Objects.equals(orderId, that.orderId) &&
                Objects.equals(customerId, that.customerId) &&
                Objects.equals(totalAmount, that.totalAmount) &&
                Objects.equals(occurredOn, that.occurredOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, customerId, totalAmount, occurredOn);
    }

    @Override
    public String toString() {
        return "OrderConfirmedEvent{" +
                "orderId=" + orderId +
                ", customerId=" + customerId +
                ", totalAmount=" + totalAmount +
                ", occurredOn=" + occurredOn +
                '}';
    }
}