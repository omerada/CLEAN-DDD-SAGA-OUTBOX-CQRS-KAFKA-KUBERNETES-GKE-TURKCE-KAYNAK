package com.example.inventory.domain.valueobject;

/**
 * Reservation Status Enum
 */
public enum ReservationStatus {
    ACTIVE("Active reservation"),
    CONFIRMED("Confirmed and allocated"),
    CANCELLED("Cancelled by business logic"),
    EXPIRED("Expired due to timeout");

    private final String description;

    ReservationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isFinal() {
        return this == CONFIRMED || this == CANCELLED || this == EXPIRED;
    }
}