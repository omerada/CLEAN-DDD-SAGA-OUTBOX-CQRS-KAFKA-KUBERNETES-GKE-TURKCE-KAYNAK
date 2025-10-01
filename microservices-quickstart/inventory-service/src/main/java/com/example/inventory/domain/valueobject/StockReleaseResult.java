package com.example.inventory.domain.valueobject;

/**
 * Stock Release Result Value Object
 */
public class StockReleaseResult {
    private final boolean successful;
    private final String message;
    private final Quantity releasedQuantity;

    private StockReleaseResult(boolean successful, String message, Quantity releasedQuantity) {
        this.successful = successful;
        this.message = message;
        this.releasedQuantity = releasedQuantity;
    }

    public static StockReleaseResult successful(Quantity releasedQuantity) {
        return new StockReleaseResult(true, "Stock released successfully", releasedQuantity);
    }

    public static StockReleaseResult failed(String message) {
        return new StockReleaseResult(false, message, null);
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }

    public Quantity getReleasedQuantity() {
        return releasedQuantity;
    }
}