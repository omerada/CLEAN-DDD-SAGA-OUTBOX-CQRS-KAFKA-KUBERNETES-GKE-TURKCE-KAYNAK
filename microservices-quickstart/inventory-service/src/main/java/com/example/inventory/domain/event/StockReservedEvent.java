package com.example.inventory.domain.event;

import com.example.inventory.domain.valueobject.*;
import java.time.LocalDateTime;

/**
 * Stock Reserved Event
 */
public class StockReservedEvent extends InventoryDomainEvent {
    private final InventoryId inventoryId;
    private final ProductId productId;
    private final ReservationId reservationId;
    private final OrderId orderId;
    private final Quantity reservedQuantity;
    private final Quantity remainingAvailable;
    private final LocalDateTime expiresAt;

    public StockReservedEvent(
            InventoryId inventoryId,
            ProductId productId,
            ReservationId reservationId,
            OrderId orderId,
            Quantity reservedQuantity,
            Quantity remainingAvailable,
            LocalDateTime expiresAt) {
        super("StockReserved", inventoryId.getValue());
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.reservedQuantity = reservedQuantity;
        this.remainingAvailable = remainingAvailable;
        this.expiresAt = expiresAt;
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

    public Quantity getReservedQuantity() {
        return reservedQuantity;
    }

    public Quantity getRemainingAvailable() {
        return remainingAvailable;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}