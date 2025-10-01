package com.example.inventory.usecase;

import com.example.inventory.domain.valueobject.ProductId;
import com.example.inventory.domain.valueobject.Quantity;
import com.example.inventory.domain.valueobject.ReservationId;

/**
 * Use case for reserving stock for an order
 * 
 * This interface defines the contract for stock reservation functionality
 * following the Clean Architecture principles.
 */
public interface ReserveStockUseCase {

    /**
     * Reserve stock for a specific product
     * 
     * @param productId the ID of the product to reserve
     * @param quantity  the quantity to reserve
     * @param orderId   the order ID for tracking the reservation
     * @return ReservationResult containing the reservation ID and status
     */
    ReservationResult reserveStock(ProductId productId, Quantity quantity, String orderId);

    /**
     * Result of a stock reservation operation
     */
    public static class ReservationResult {
        private final boolean success;
        private final ReservationId reservationId;
        private final String message;
        private final Quantity reservedQuantity;

        private ReservationResult(boolean success, ReservationId reservationId,
                String message, Quantity reservedQuantity) {
            this.success = success;
            this.reservationId = reservationId;
            this.message = message;
            this.reservedQuantity = reservedQuantity;
        }

        public static ReservationResult success(ReservationId reservationId,
                Quantity reservedQuantity) {
            return new ReservationResult(true, reservationId, "Stock reserved successfully",
                    reservedQuantity);
        }

        public static ReservationResult failure(String message) {
            return new ReservationResult(false, null, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public ReservationId getReservationId() {
            return reservationId;
        }

        public String getMessage() {
            return message;
        }

        public Quantity getReservedQuantity() {
            return reservedQuantity;
        }
    }
}