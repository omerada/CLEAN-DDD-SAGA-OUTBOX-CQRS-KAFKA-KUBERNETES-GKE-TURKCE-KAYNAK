package com.example.inventory.usecase;

import com.example.inventory.domain.valueobject.ReservationId;

/**
 * Use case for cancelling a stock reservation
 * 
 * This interface defines the contract for cancelling reservations
 * when an order is cancelled or fails.
 */
public interface CancelReservationUseCase {

    /**
     * Cancel a stock reservation (return reserved stock to available)
     * 
     * @param reservationId the ID of the reservation to cancel
     * @param reason        the reason for cancellation
     * @return CancellationResult containing the cancellation status
     */
    CancellationResult cancelReservation(ReservationId reservationId, String reason);

    /**
     * Result of a reservation cancellation operation
     */
    public static class CancellationResult {
        private final boolean success;
        private final String message;
        private final ReservationId reservationId;
        private final String reason;

        private CancellationResult(boolean success, String message,
                ReservationId reservationId, String reason) {
            this.success = success;
            this.message = message;
            this.reservationId = reservationId;
            this.reason = reason;
        }

        public static CancellationResult success(ReservationId reservationId, String reason) {
            return new CancellationResult(true, "Reservation cancelled successfully",
                    reservationId, reason);
        }

        public static CancellationResult failure(String message) {
            return new CancellationResult(false, message, null, null);
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

        public String getReason() {
            return reason;
        }
    }
}