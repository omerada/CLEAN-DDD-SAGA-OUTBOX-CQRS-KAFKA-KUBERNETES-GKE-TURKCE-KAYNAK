package com.example.inventory.domain.entity;

import com.example.inventory.domain.valueobject.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Objects;

/**
 * Stock Reservation Entity
 * 
 * Represents a temporary stock reservation for an order.
 * Business Rules:
 * - Reservations have expiration time
 * - Active reservations can be confirmed or cancelled
 * - Expired reservations automatically release stock
 * - Status transitions follow business rules
 */
@Entity
@Table(name = "stock_reservations", indexes = {
        @Index(name = "idx_reservation_order", columnList = "orderId"),
        @Index(name = "idx_reservation_status", columnList = "status"),
        @Index(name = "idx_reservation_expires", columnList = "expiresAt"),
        @Index(name = "idx_reservation_inventory", columnList = "inventory_id")
})
public class StockReservation {

    @Id
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Version
    @Column(name = "version")
    private Long version;

    protected StockReservation() {
    } // JPA constructor

    private StockReservation(
            ReservationId reservationId,
            Inventory inventory,
            OrderId orderId,
            Quantity quantity,
            LocalDateTime expiresAt) {
        this.id = reservationId.getValue();
        this.inventory = inventory;
        this.orderId = orderId.getValue();
        this.quantity = quantity.getValue();
        this.status = ReservationStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.version = 0L;
    }

    /**
     * Factory method - Create new reservation
     */
    public static StockReservation create(
            ReservationId reservationId,
            Inventory inventory,
            OrderId orderId,
            Quantity quantity,
            LocalDateTime expiresAt) {
        if (reservationId == null || inventory == null || orderId == null || quantity == null) {
            throw new IllegalArgumentException("All parameters are required for reservation creation");
        }
        if (expiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Expiration time cannot be in the past");
        }
        if (quantity.getValue() <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be positive");
        }

        return new StockReservation(reservationId, inventory, orderId, quantity, expiresAt);
    }

    /**
     * Confirm reservation - convert to allocation
     */
    public ConfirmationResult confirm() {
        if (this.status != ReservationStatus.ACTIVE) {
            return ConfirmationResult.failed("Only active reservations can be confirmed");
        }

        if (isExpired()) {
            return ConfirmationResult.failed("Cannot confirm expired reservation");
        }

        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();

        return ConfirmationResult.successful();
    }

    /**
     * Cancel reservation
     */
    public CancellationResult cancel(String reason) {
        if (this.status != ReservationStatus.ACTIVE) {
            return CancellationResult.failed("Only active reservations can be cancelled");
        }

        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;

        return CancellationResult.successful();
    }

    /**
     * Expire reservation due to timeout
     */
    public void expire() {
        if (this.status == ReservationStatus.ACTIVE) {
            this.status = ReservationStatus.EXPIRED;
            this.cancelledAt = LocalDateTime.now();
            this.cancellationReason = "Reservation expired";
        }
    }

    /**
     * Check if reservation is expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * Get remaining time before expiration
     */
    public Duration getRemainingTime() {
        if (isExpired()) {
            return Duration.ZERO;
        }
        return Duration.between(LocalDateTime.now(), this.expiresAt);
    }

    /**
     * Check if reservation is active and not expired
     */
    public boolean isActiveAndValid() {
        return this.status == ReservationStatus.ACTIVE && !isExpired();
    }

    // Getters (no setters - immutability through behavior)
    public ReservationId getId() {
        return ReservationId.of(this.id);
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public OrderId getOrderId() {
        return OrderId.of(this.orderId);
    }

    public Quantity getQuantity() {
        return Quantity.of(this.quantity);
    }

    public ReservationStatus getStatus() {
        return this.status;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return this.expiresAt;
    }

    public LocalDateTime getConfirmedAt() {
        return this.confirmedAt;
    }

    public LocalDateTime getCancelledAt() {
        return this.cancelledAt;
    }

    public String getCancellationReason() {
        return this.cancellationReason;
    }

    public Long getVersion() {
        return this.version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        StockReservation that = (StockReservation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("StockReservation{id='%s', orderId='%s', quantity=%d, status=%s}",
                id, orderId, quantity, status);
    }

    // Inner result classes
    public static class ConfirmationResult {
        private final boolean successful;
        private final String message;

        private ConfirmationResult(boolean successful, String message) {
            this.successful = successful;
            this.message = message;
        }

        public static ConfirmationResult successful() {
            return new ConfirmationResult(true, "Reservation confirmed successfully");
        }

        public static ConfirmationResult failed(String message) {
            return new ConfirmationResult(false, message);
        }

        public boolean isSuccessful() {
            return successful;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class CancellationResult {
        private final boolean successful;
        private final String message;

        private CancellationResult(boolean successful, String message) {
            this.successful = successful;
            this.message = message;
        }

        public static CancellationResult successful() {
            return new CancellationResult(true, "Reservation cancelled successfully");
        }

        public static CancellationResult failed(String message) {
            return new CancellationResult(false, message);
        }

        public boolean isSuccessful() {
            return successful;
        }

        public String getMessage() {
            return message;
        }
    }

    // Additional methods for application service compatibility
    public static StockReservation create(ReservationId reservationId, ProductId productId,
            Quantity quantity, String orderId, LocalDateTime createdAt) {
        StockReservation reservation = new StockReservation();
        reservation.id = reservationId.getValue();
        reservation.orderId = orderId;
        reservation.quantity = quantity.getValue();
        reservation.status = ReservationStatus.ACTIVE;
        reservation.createdAt = createdAt;
        reservation.expiresAt = createdAt.plusMinutes(30); // Default 30 minutes expiry
        reservation.version = 0L;
        return reservation;
    }

    public ProductId getProductId() {
        return inventory != null ? inventory.getProductId() : null;
    }

    // Domain events support (simplified)
    public java.util.List<com.example.inventory.domain.event.DomainEvent> getUncommittedEvents() {
        return new java.util.ArrayList<>(); // Simplified for now
    }

    public void markEventsAsCommitted() {
        // Simplified for now
    }

    /**
     * Check if reservation can be confirmed
     */
    public boolean canBeConfirmed() {
        return this.status == ReservationStatus.ACTIVE && !isExpired();
    }

    /**
     * Check if reservation can be cancelled
     */
    public boolean canBeCancelled() {
        return this.status == ReservationStatus.ACTIVE;
    }
}