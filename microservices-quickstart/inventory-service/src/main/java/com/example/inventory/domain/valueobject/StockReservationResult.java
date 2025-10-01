package com.example.inventory.domain.valueobject;

import java.time.LocalDateTime;

/**
 * Stock Reservation Result Value Object
 */
public class StockReservationResult {
    private final boolean successful;
    private final String message;
    private final ReservationId reservationId;
    private final LocalDateTime expiresAt;

    private StockReservationResult(boolean successful, String message, ReservationId reservationId,
            LocalDateTime expiresAt) {
        this.successful = successful;
        this.message = message;
        this.reservationId = reservationId;
        this.expiresAt = expiresAt;
    }

    public static StockReservationResult successful(ReservationId reservationId, LocalDateTime expiresAt) {
        return new StockReservationResult(true, "Stock reserved successfully", reservationId, expiresAt);
    }

    public static StockReservationResult failed(String message) {
        return new StockReservationResult(false, message, null, null);
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }

    public ReservationId getReservationId() {
        return reservationId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}