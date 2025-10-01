package com.example.inventory.domain.event;

import com.example.inventory.domain.valueobject.*;

/**
 * Stock Allocated Event
 */
public class StockAllocatedEvent extends InventoryDomainEvent {
    private final InventoryId inventoryId;
    private final ProductId productId;
    private final ReservationId reservationId;
    private final OrderId orderId;
    private final Quantity allocatedQuantity;
    private final Quantity newReservedQuantity;

    public StockAllocatedEvent(
            InventoryId inventoryId,
            ProductId productId,
            ReservationId reservationId,
            OrderId orderId,
            Quantity allocatedQuantity,
            Quantity newReservedQuantity) {
        super("StockAllocated", inventoryId.getValue());
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.allocatedQuantity = allocatedQuantity;
        this.newReservedQuantity = newReservedQuantity;
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

    public Quantity getAllocatedQuantity() {
        return allocatedQuantity;
    }

    public Quantity getNewReservedQuantity() {
        return newReservedQuantity;
    }
}