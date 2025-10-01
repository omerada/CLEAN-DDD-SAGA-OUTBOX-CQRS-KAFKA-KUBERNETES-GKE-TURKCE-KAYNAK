package com.example.inventory.domain.event;

import com.example.inventory.domain.valueobject.*;

/**
 * Inventory Created Event
 */
public class InventoryCreatedEvent extends InventoryDomainEvent {
    private final InventoryId inventoryId;
    private final ProductId productId;
    private final Quantity totalQuantity;
    private final Quantity availableQuantity;

    public InventoryCreatedEvent(
            InventoryId inventoryId,
            ProductId productId,
            Quantity totalQuantity,
            Quantity availableQuantity) {
        super("InventoryCreated", inventoryId.getValue());
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = availableQuantity;
    }

    public InventoryId getInventoryId() {
        return inventoryId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public Quantity getTotalQuantity() {
        return totalQuantity;
    }

    public Quantity getAvailableQuantity() {
        return availableQuantity;
    }
}