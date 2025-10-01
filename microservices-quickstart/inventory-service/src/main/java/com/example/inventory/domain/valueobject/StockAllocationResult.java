package com.example.inventory.domain.valueobject;

/**
 * Stock Allocation Result Value Object
 */
public class StockAllocationResult {
    private final boolean successful;
    private final String message;
    private final Quantity allocatedQuantity;

    private StockAllocationResult(boolean successful, String message, Quantity allocatedQuantity) {
        this.successful = successful;
        this.message = message;
        this.allocatedQuantity = allocatedQuantity;
    }

    public static StockAllocationResult successful(Quantity allocatedQuantity) {
        return new StockAllocationResult(true, "Stock allocated successfully", allocatedQuantity);
    }

    public static StockAllocationResult failed(String message) {
        return new StockAllocationResult(false, message, null);
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }

    public Quantity getAllocatedQuantity() {
        return allocatedQuantity;
    }
}