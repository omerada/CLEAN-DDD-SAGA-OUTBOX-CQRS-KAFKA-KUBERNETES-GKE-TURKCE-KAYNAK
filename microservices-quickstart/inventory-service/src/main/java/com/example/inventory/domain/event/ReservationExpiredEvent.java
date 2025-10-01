package com.example.inventory.domain.event;

import com.example.inventory.domain.valueobject.*;

/**
 * Reservation Expired Event
 */
public class ReservationExpiredEvent extends InventoryDomainEvent {
    private final InventoryId inventoryId;
    private final ProductId productId;
    private final ReservationId reservationId;
    private final OrderId orderId;
    private final Quantity expiredQuantity;
    private final Quantity newAvailableQuantity;

    public ReservationExpiredEvent(
            InventoryId inventoryId,
            ProductId productId,
            ReservationId reservationId,
            OrderId orderId,
            Quantity expiredQuantity,
            Quantity newAvailableQuantity) {
        super("ReservationExpired", inventoryId.getValue());
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.expiredQuantity = expiredQuantity;
        this.newAvailableQuantity = newAvailableQuantity;
    }

    // Getters
    public InventoryId getInventoryId() {
        return inventoryId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public ReservationId getReservationId() {
        return reservationId;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public Quantity getExpiredQuantity() {
        return expiredQuantity;
    }

    public Quantity getNewAvailableQuantity() {
        return newAvailableQuantity;
    }
}