package com.example.inventory.usecase;

import com.example.inventory.domain.valueobject.ReservationId;

/**
 * Use case for confirming a stock reservation
 * 
 * This interface defines the contract for confirming reservations
 * when an order is confirmed or fulfilled.
 */
public interface ConfirmReservationUseCase {

    /**
     * Confirm a stock reservation (convert reserved stock to confirmed allocation)
     * 
     * @param reservationId the ID of the reservation to confirm
     * @return ConfirmationResult containing the confirmation status
     */
    ConfirmationResult confirmReservation(ReservationId reservationId);

    /**
     * Result of a reservation confirmation operation
     */
    public static class ConfirmationResult {
        private final boolean success;
        private final String message;
        private final ReservationId reservationId;

        private ConfirmationResult(boolean success, String message, ReservationId reservationId) {
            this.success = success;
            this.message = message;
            this.reservationId = reservationId;
        }

        public static ConfirmationResult success(ReservationId reservationId) {
            return new ConfirmationResult(true, "Reservation confirmed successfully", reservationId);
        }

        public static ConfirmationResult failure(String message) {
            return new ConfirmationResult(false, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public ReservationId getReservationId() {
            return reservationId;
        }
    }
}