package com.example.inventory.domain.valueobject;

/**
 * Restock Result Value Object
 */
public class RestockResult {
    private final boolean successful;
    private final String message;
    private final Quantity newTotalQuantity;
    private final Quantity newAvailableQuantity;

    private RestockResult(boolean successful, String message, Quantity newTotalQuantity,
            Quantity newAvailableQuantity) {
        this.successful = successful;
        this.message = message;
        this.newTotalQuantity = newTotalQuantity;
        this.newAvailableQuantity = newAvailableQuantity;
    }

    public static RestockResult successful(Quantity newTotalQuantity, Quantity newAvailableQuantity) {
        return new RestockResult(true, "Stock added successfully", newTotalQuantity, newAvailableQuantity);
    }

    public static RestockResult failed(String message) {
        return new RestockResult(false, message, null, null);
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }

    public Quantity getNewTotalQuantity() {
        return newTotalQuantity;
    }

    public Quantity getNewAvailableQuantity() {
        return newAvailableQuantity;
    }
}