package com.example.inventory.domain.event;

import com.example.inventory.domain.valueobject.*;

/**
 * Low Stock Detected Event
 */
public class LowStockDetectedEvent extends InventoryDomainEvent {
    private final InventoryId inventoryId;
    private final ProductId productId;
    private final Quantity currentQuantity;
    private final Quantity reorderPoint;

    public LowStockDetectedEvent(
            InventoryId inventoryId,
            ProductId productId,
            Quantity currentQuantity,
            Quantity reorderPoint) {
        super("LowStockDetected", inventoryId.getValue());
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.currentQuantity = currentQuantity;
        this.reorderPoint = reorderPoint;
    }

    // Getters
    public InventoryId getInventoryId() {
        return inventoryId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public Quantity getCurrentQuantity() {
        return currentQuantity;
    }

    public Quantity getReorderPoint() {
        return reorderPoint;
    }
}