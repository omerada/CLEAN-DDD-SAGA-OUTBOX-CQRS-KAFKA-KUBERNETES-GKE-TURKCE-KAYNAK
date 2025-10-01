package com.example.inventory.domain.event;

import com.example.inventory.domain.valueobject.*;

/**
 * Stock Added Event
 */
public class StockAddedEvent extends InventoryDomainEvent {
    private final InventoryId inventoryId;
    private final ProductId productId;
    private final Quantity addedQuantity;
    private final Quantity previousTotal;
    private final Quantity newTotal;
    private final Quantity previousAvailable;
    private final Quantity newAvailable;
    private final String reason;
    private final String addedBy;

    public StockAddedEvent(
            InventoryId inventoryId,
            ProductId productId,
            Quantity addedQuantity,
            Quantity previousTotal,
            Quantity newTotal,
            Quantity previousAvailable,
            Quantity newAvailable,
            String reason,
            String addedBy) {
        super("StockAdded", inventoryId.getValue());
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.addedQuantity = addedQuantity;
        this.previousTotal = previousTotal;
        this.newTotal = newTotal;
        this.previousAvailable = previousAvailable;
        this.newAvailable = newAvailable;
        this.reason = reason;
        this.addedBy = addedBy;
    }

    // Getters
    public InventoryId getInventoryId() {
        return inventoryId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public Quantity getAddedQuantity() {
        return addedQuantity;
    }

    public Quantity getPreviousTotal() {
        return previousTotal;
    }

    public Quantity getNewTotal() {
        return newTotal;
    }

    public Quantity getPreviousAvailable() {
        return previousAvailable;
    }

    public Quantity getNewAvailable() {
        return newAvailable;
    }

    public String getReason() {
        return reason;
    }

    public String getAddedBy() {
        return addedBy;
    }
}