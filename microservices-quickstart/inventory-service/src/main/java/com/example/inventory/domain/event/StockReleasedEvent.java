package com.example.inventory.domain.event;

import com.example.inventory.domain.valueobject.*;

/**
 * Stock Released Event
 */
public class StockReleasedEvent extends InventoryDomainEvent {
    private final InventoryId inventoryId;
    private final ProductId productId;
    private final ReservationId reservationId;
    private final OrderId orderId;
    private final Quantity releasedQuantity;
    private final Quantity newAvailableQuantity;
    private final String reason;

    public StockReleasedEvent(
            InventoryId inventoryId,
            ProductId productId,
            ReservationId reservationId,
            OrderId orderId,
            Quantity releasedQuantity,
            Quantity newAvailableQuantity,
            String reason) {
        super("StockReleased", inventoryId.getValue());
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.releasedQuantity = releasedQuantity;
        this.newAvailableQuantity = newAvailableQuantity;
        this.reason = reason;
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

    public Quantity getReleasedQuantity() {
        return releasedQuantity;
    }

    public Quantity getNewAvailableQuantity() {
        return newAvailableQuantity;
    }

    public String getReason() {
        return reason;
    }
}