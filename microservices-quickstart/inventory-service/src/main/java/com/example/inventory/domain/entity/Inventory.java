package com.example.inventory.domain.entity;

import com.example.inventory.domain.valueobject.*;
import com.example.inventory.domain.event.InventoryDomainEvent;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;

/**
 * Inventory Aggregate Root
 * 
 * CLEAN VERSION - Events temporarily disabled for compilation
 */
@Entity
@Table(name = "inventories", indexes = {
        @Index(name = "idx_inventory_product", columnList = "productId"),
        @Index(name = "idx_inventory_available", columnList = "availableQuantity")
})
public class Inventory {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Column(name = "reorder_point", nullable = false)
    private Integer reorderPoint;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @Transient
    private List<InventoryDomainEvent> domainEvents = new ArrayList<>();

    protected Inventory() {
    } // JPA constructor

    private Inventory(ProductId productId, Quantity totalQuantity, Quantity minimumLevel) {
        this.id = InventoryId.generate().getValue();
        this.productId = productId.getValue();
        this.totalQuantity = totalQuantity.getValue();
        this.availableQuantity = totalQuantity.getValue();
        this.reservedQuantity = 0;
        this.reorderPoint = minimumLevel.getValue();
        this.createdAt = LocalDateTime.now();
        this.version = 0L;
    }

    /**
     * Factory method - Create new inventory
     */
    public static Inventory create(ProductId productId, Quantity totalQuantity, Quantity minimumLevel) {
        if (productId == null || totalQuantity == null || minimumLevel == null) {
            throw new IllegalArgumentException("All parameters are required for inventory creation");
        }
        if (totalQuantity.getValue() <= 0) {
            throw new IllegalArgumentException("Total quantity must be positive");
        }
        if (minimumLevel.getValue() < 0) {
            throw new IllegalArgumentException("Minimum level cannot be negative");
        }

        Inventory inventory = new Inventory(productId, totalQuantity, minimumLevel);
        // TODO: Add InventoryCreatedEvent when events are fixed
        return inventory;
    }

    /**
     * Reserve stock for order
     */
    public StockReservationResult reserveStock(
            Quantity requestedQuantity,
            OrderId orderId,
            LocalDateTime expiresAt) {

        if (requestedQuantity.getValue() <= 0) {
            return StockReservationResult.failed("Requested quantity must be positive");
        }

        if (this.availableQuantity < requestedQuantity.getValue()) {
            return StockReservationResult.failed("Insufficient stock available");
        }

        // Update quantities
        this.availableQuantity -= requestedQuantity.getValue();
        this.reservedQuantity += requestedQuantity.getValue();
        this.updatedAt = LocalDateTime.now();

        // TODO: Add StockReservedEvent when events are fixed
        // TODO: Add LowStockDetectedEvent check when events are fixed

        return StockReservationResult.successful(ReservationId.generate());
    }

    /**
     * Release reserved stock
     */
    public StockReleaseResult releaseReservedStock(Quantity quantity, String reason) {
        if (quantity.getValue() <= 0) {
            return StockReleaseResult.failed("Quantity must be positive");
        }

        if (this.reservedQuantity < quantity.getValue()) {
            return StockReleaseResult.failed("Cannot release more than reserved quantity");
        }

        // Update quantities
        this.reservedQuantity -= quantity.getValue();
        this.availableQuantity += quantity.getValue();
        this.updatedAt = LocalDateTime.now();

        // TODO: Add StockReleasedEvent when events are fixed

        return StockReleaseResult.successful();
    }

    /**
     * Allocate reserved stock (confirm reservation)
     */
    public StockAllocationResult allocateReservedStock(Quantity quantity) {
        if (quantity.getValue() <= 0) {
            return StockAllocationResult.failed("Quantity must be positive");
        }

        if (this.reservedQuantity < quantity.getValue()) {
            return StockAllocationResult.failed("Cannot allocate more than reserved quantity");
        }

        // Update quantities
        this.reservedQuantity -= quantity.getValue();
        this.totalQuantity -= quantity.getValue();
        this.updatedAt = LocalDateTime.now();

        // TODO: Add StockAllocatedEvent when events are fixed

        return StockAllocationResult.successful();
    }

    /**
     * Add stock to inventory
     */
    public StockAdditionResult addStock(Quantity quantity, String reason, String addedBy) {
        if (quantity.getValue() <= 0) {
            return StockAdditionResult.failed("Quantity must be positive");
        }

        int previousTotal = this.totalQuantity;
        int previousAvailable = this.availableQuantity;

        // Update quantities
        this.totalQuantity += quantity.getValue();
        this.availableQuantity += quantity.getValue();
        this.updatedAt = LocalDateTime.now();

        // TODO: Add StockAddedEvent when events are fixed

        return StockAdditionResult.successful();
    }

    /**
     * Check if stock can be reserved
     */
    public boolean canReserve(Quantity quantity) {
        return this.availableQuantity >= quantity.getValue();
    }

    /**
     * Confirm reservation (allocate reserved stock)
     */
    public StockAllocationResult confirmReservation(Quantity quantity) {
        return allocateReservedStock(quantity);
    }

    /**
     * Cancel reservation (release reserved stock)
     */
    public StockReleaseResult cancelReservation(Quantity quantity) {
        return releaseReservedStock(quantity, "Reservation cancelled");
    }

    // Getters
    public InventoryId getInventoryId() {
        return InventoryId.of(this.id);
    }

    public ProductId getProductId() {
        return ProductId.of(this.productId);
    }

    public Quantity getTotalQuantity() {
        return Quantity.of(this.totalQuantity);
    }

    public Quantity getAvailableQuantity() {
        return Quantity.of(this.availableQuantity);
    }

    public Quantity getReservedQuantity() {
        return Quantity.of(this.reservedQuantity);
    }

    public Quantity getReorderPoint() {
        return Quantity.of(this.reorderPoint);
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public Long getVersion() {
        return this.version;
    }

    // Domain events support
    public List<InventoryDomainEvent> getUncommittedEvents() {
        return new ArrayList<>(this.domainEvents);
    }

    public void markEventsAsCommitted() {
        this.domainEvents.clear();
    }

    private void addDomainEvent(InventoryDomainEvent event) {
        this.domainEvents.add(event);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Inventory inventory = (Inventory) o;
        return Objects.equals(id, inventory.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Inventory{id='%s', productId='%s', total=%d, available=%d, reserved=%d}",
                id, productId, totalQuantity, availableQuantity, reservedQuantity);
    }

    // Result classes
    public static class StockReservationResult {
        private final boolean successful;
        private final String message;
        private final ReservationId reservationId;

        private StockReservationResult(boolean successful, String message, ReservationId reservationId) {
            this.successful = successful;
            this.message = message;
            this.reservationId = reservationId;
        }

        public static StockReservationResult successful(ReservationId reservationId) {
            return new StockReservationResult(true, "Stock reserved successfully", reservationId);
        }

        public static StockReservationResult failed(String message) {
            return new StockReservationResult(false, message, null);
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
    }

    public static class StockReleaseResult {
        private final boolean successful;
        private final String message;

        private StockReleaseResult(boolean successful, String message) {
            this.successful = successful;
            this.message = message;
        }

        public static StockReleaseResult successful() {
            return new StockReleaseResult(true, "Stock released successfully");
        }

        public static StockReleaseResult failed(String message) {
            return new StockReleaseResult(false, message);
        }

        public boolean isSuccessful() {
            return successful;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class StockAllocationResult {
        private final boolean successful;
        private final String message;

        private StockAllocationResult(boolean successful, String message) {
            this.successful = successful;
            this.message = message;
        }

        public static StockAllocationResult successful() {
            return new StockAllocationResult(true, "Stock allocated successfully");
        }

        public static StockAllocationResult failed(String message) {
            return new StockAllocationResult(false, message);
        }

        public boolean isSuccessful() {
            return successful;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class StockAdditionResult {
        private final boolean successful;
        private final String message;

        private StockAdditionResult(boolean successful, String message) {
            this.successful = successful;
            this.message = message;
        }

        public static StockAdditionResult successful() {
            return new StockAdditionResult(true, "Stock added successfully");
        }

        public static StockAdditionResult failed(String message) {
            return new StockAdditionResult(false, message);
        }

        public boolean isSuccessful() {
            return successful;
        }

        public String getMessage() {
            return message;
        }
    }
}