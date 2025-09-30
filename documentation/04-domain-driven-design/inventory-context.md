# 🎯 Inventory Bounded Context - DDD Implementation

## Inventory Aggregate Design

### Inventory Aggregate Root

```java
// domain/model/Inventory.java
package com.example.inventory.domain.model;

import com.example.inventory.domain.valueobject.*;
import com.example.inventory.domain.event.*;
import com.example.inventory.domain.policy.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Inventory Aggregate Root
 *
 * Business Rules:
 * - Stock levels cannot go negative
 * - Reservations must be tracked with expiration
 * - Low stock alerts must be triggered
 * - Stock adjustments require audit trail
 * - Concurrent reservations must be handled safely
 */
public class Inventory {
    private InventoryId id;
    private ProductId productId;
    private StockLevel currentStock;
    private StockLevel reservedStock;
    private StockLevel availableStock;
    private ReorderPoint reorderPoint;
    private List<StockReservation> activeReservations;
    private List<StockMovement> movements;
    private InventoryPolicy inventoryPolicy;
    private LocalDateTime lastUpdated;

    // Domain events
    private List<DomainEvent> domainEvents = new ArrayList<>();

    private Inventory() {
        this.activeReservations = new ArrayList<>();
        this.movements = new ArrayList<>();
    }

    /**
     * Factory method for creating new inventory
     */
    public static Inventory initialize(
        ProductId productId,
        StockLevel initialStock,
        ReorderPoint reorderPoint,
        InventoryPolicy inventoryPolicy
    ) {
        validateInitialization(productId, initialStock, reorderPoint, inventoryPolicy);

        Inventory inventory = new Inventory();
        inventory.id = InventoryId.generate();
        inventory.productId = productId;
        inventory.currentStock = initialStock;
        inventory.reservedStock = StockLevel.ZERO;
        inventory.availableStock = initialStock;
        inventory.reorderPoint = reorderPoint;
        inventory.inventoryPolicy = inventoryPolicy;
        inventory.lastUpdated = LocalDateTime.now();

        // Record initial stock movement
        inventory.addStockMovement(StockMovement.initialStock(
            inventory.id,
            initialStock,
            "Initial inventory setup",
            LocalDateTime.now()
        ));

        inventory.addDomainEvent(new InventoryInitializedEvent(
            inventory.id,
            inventory.productId,
            initialStock,
            inventory.lastUpdated
        ));

        return inventory;
    }

    /**
     * Reserve stock for an order
     */
    public ReservationResult reserveStock(
        Quantity requestedQuantity,
        OrderId orderId,
        Duration reservationDuration
    ) {
        if (requestedQuantity == null || requestedQuantity.isNegativeOrZero()) {
            return ReservationResult.failed("Requested quantity must be positive");
        }

        // Check if enough stock available
        if (availableStock.getQuantity().isLessThan(requestedQuantity)) {
            return ReservationResult.failed(
                String.format("Insufficient stock. Available: %d, Requested: %d",
                    availableStock.getQuantity().getValue(),
                    requestedQuantity.getValue())
            );
        }

        // Apply inventory policies
        ReservationPolicyResult policyResult = inventoryPolicy.canReserve(
            this, requestedQuantity, orderId
        );

        if (!policyResult.isAllowed()) {
            return ReservationResult.failed(policyResult.getDenialReason());
        }

        // Create reservation
        StockReservation reservation = StockReservation.create(
            ReservationId.generate(),
            this.id,
            this.productId,
            orderId,
            requestedQuantity,
            LocalDateTime.now().plus(reservationDuration)
        );

        activeReservations.add(reservation);

        // Update stock levels
        StockLevel reservedAmount = StockLevel.of(requestedQuantity);
        this.reservedStock = this.reservedStock.add(reservedAmount);
        this.availableStock = this.availableStock.subtract(reservedAmount);
        this.lastUpdated = LocalDateTime.now();

        // Record stock movement
        addStockMovement(StockMovement.reservation(
            this.id,
            reservedAmount,
            orderId.getValue(),
            LocalDateTime.now()
        ));

        // Publish domain event
        addDomainEvent(new StockReservedEvent(
            this.id,
            this.productId,
            orderId,
            reservation.getId(),
            requestedQuantity,
            reservation.getExpiresAt(),
            this.lastUpdated
        ));

        // Check for low stock alert
        checkAndTriggerLowStockAlert();

        return ReservationResult.successful(reservation.getId(), reservation.getExpiresAt());
    }

    /**
     * Confirm reservation (convert to allocated)
     */
    public void confirmReservation(ReservationId reservationId) {
        StockReservation reservation = findActiveReservation(reservationId)
            .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        // Remove from active reservations
        activeReservations.remove(reservation);

        // Update stock levels - reserved becomes allocated (permanently removed)
        StockLevel allocatedAmount = StockLevel.of(reservation.getQuantity());
        this.reservedStock = this.reservedStock.subtract(allocatedAmount);
        this.currentStock = this.currentStock.subtract(allocatedAmount);
        this.lastUpdated = LocalDateTime.now();

        // Record stock movement
        addStockMovement(StockMovement.allocation(
            this.id,
            allocatedAmount,
            reservation.getOrderId().getValue(),
            LocalDateTime.now()
        ));

        addDomainEvent(new StockAllocatedEvent(
            this.id,
            this.productId,
            reservation.getOrderId(),
            reservationId,
            reservation.getQuantity(),
            this.lastUpdated
        ));
    }

    /**
     * Cancel reservation (release reserved stock)
     */
    public void cancelReservation(ReservationId reservationId, String reason) {
        StockReservation reservation = findActiveReservation(reservationId)
            .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        // Remove from active reservations
        activeReservations.remove(reservation);

        // Release reserved stock back to available
        StockLevel releasedAmount = StockLevel.of(reservation.getQuantity());
        this.reservedStock = this.reservedStock.subtract(releasedAmount);
        this.availableStock = this.availableStock.add(releasedAmount);
        this.lastUpdated = LocalDateTime.now();

        // Record stock movement
        addStockMovement(StockMovement.reservationCancellation(
            this.id,
            releasedAmount,
            reason,
            LocalDateTime.now()
        ));

        addDomainEvent(new ReservationCancelledEvent(
            this.id,
            this.productId,
            reservation.getOrderId(),
            reservationId,
            reservation.getQuantity(),
            reason,
            this.lastUpdated
        ));
    }

    /**
     * Add stock (receive new inventory)
     */
    public void addStock(Quantity quantity, String reason, String sourceReference) {
        if (quantity == null || quantity.isNegativeOrZero()) {
            throw new IllegalArgumentException("Quantity to add must be positive");
        }

        StockLevel addedStock = StockLevel.of(quantity);
        this.currentStock = this.currentStock.add(addedStock);
        this.availableStock = this.availableStock.add(addedStock);
        this.lastUpdated = LocalDateTime.now();

        // Record stock movement
        addStockMovement(StockMovement.stockIncrease(
            this.id,
            addedStock,
            reason,
            sourceReference,
            LocalDateTime.now()
        ));

        addDomainEvent(new StockIncreasedEvent(
            this.id,
            this.productId,
            quantity,
            reason,
            sourceReference,
            this.lastUpdated
        ));
    }

    /**
     * Remove stock (damage, theft, etc.)
     */
    public void removeStock(Quantity quantity, String reason, String reference) {
        if (quantity == null || quantity.isNegativeOrZero()) {
            throw new IllegalArgumentException("Quantity to remove must be positive");
        }

        StockLevel removedStock = StockLevel.of(quantity);

        if (this.availableStock.getQuantity().isLessThan(quantity)) {
            throw new InsufficientStockException(
                String.format("Cannot remove %d units. Available stock: %d",
                    quantity.getValue(),
                    this.availableStock.getQuantity().getValue())
            );
        }

        this.currentStock = this.currentStock.subtract(removedStock);
        this.availableStock = this.availableStock.subtract(removedStock);
        this.lastUpdated = LocalDateTime.now();

        // Record stock movement
        addStockMovement(StockMovement.stockDecrease(
            this.id,
            removedStock,
            reason,
            reference,
            LocalDateTime.now()
        ));

        addDomainEvent(new StockDecreasedEvent(
            this.id,
            this.productId,
            quantity,
            reason,
            reference,
            this.lastUpdated
        ));

        // Check for low stock alert
        checkAndTriggerLowStockAlert();
    }

    /**
     * Process expired reservations
     */
    public List<ReservationId> processExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<ReservationId> expiredReservations = new ArrayList<>();

        Iterator<StockReservation> iterator = activeReservations.iterator();
        while (iterator.hasNext()) {
            StockReservation reservation = iterator.next();
            if (reservation.isExpired(now)) {
                // Release reserved stock
                StockLevel releasedAmount = StockLevel.of(reservation.getQuantity());
                this.reservedStock = this.reservedStock.subtract(releasedAmount);
                this.availableStock = this.availableStock.add(releasedAmount);

                iterator.remove();
                expiredReservations.add(reservation.getId());

                // Record movement
                addStockMovement(StockMovement.expiredReservation(
                    this.id,
                    releasedAmount,
                    reservation.getId().getValue(),
                    now
                ));

                addDomainEvent(new ReservationExpiredEvent(
                    this.id,
                    this.productId,
                    reservation.getOrderId(),
                    reservation.getId(),
                    reservation.getQuantity(),
                    now
                ));
            }
        }

        if (!expiredReservations.isEmpty()) {
            this.lastUpdated = now;
        }

        return expiredReservations;
    }

    /**
     * Check availability for future orders
     */
    public AvailabilityResult checkAvailability(Quantity requestedQuantity, LocalDateTime targetDate) {
        // Consider current available stock
        Quantity currentlyAvailable = this.availableStock.getQuantity();

        if (currentlyAvailable.isGreaterThanOrEqual(requestedQuantity)) {
            return AvailabilityResult.available(currentlyAvailable);
        }

        // Check expected stock based on reservations expiring before target date
        Quantity expectedAvailable = calculateExpectedAvailableStock(targetDate);

        if (expectedAvailable.isGreaterThanOrEqual(requestedQuantity)) {
            return AvailabilityResult.availableByDate(expectedAvailable, targetDate);
        }

        // Calculate shortage
        Quantity shortage = requestedQuantity.subtract(expectedAvailable);
        return AvailabilityResult.insufficient(expectedAvailable, shortage);
    }

    private void checkAndTriggerLowStockAlert() {
        if (this.availableStock.getQuantity().isLessThanOrEqual(this.reorderPoint.getQuantity())) {
            addDomainEvent(new LowStockAlertEvent(
                this.id,
                this.productId,
                this.availableStock.getQuantity(),
                this.reorderPoint.getQuantity(),
                this.lastUpdated
            ));
        }
    }

    private Quantity calculateExpectedAvailableStock(LocalDateTime targetDate) {
        Quantity expectedReleased = activeReservations.stream()
            .filter(reservation -> reservation.getExpiresAt().isBefore(targetDate))
            .map(StockReservation::getQuantity)
            .reduce(Quantity.ZERO, Quantity::add);

        return this.availableStock.getQuantity().add(expectedReleased);
    }

    private Optional<StockReservation> findActiveReservation(ReservationId reservationId) {
        return activeReservations.stream()
            .filter(reservation -> reservation.getId().equals(reservationId))
            .findFirst();
    }

    private void addStockMovement(StockMovement movement) {
        this.movements.add(movement);
    }

    private void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    private static void validateInitialization(
        ProductId productId,
        StockLevel initialStock,
        ReorderPoint reorderPoint,
        InventoryPolicy inventoryPolicy
    ) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required");
        }
        if (initialStock == null) {
            throw new IllegalArgumentException("Initial stock level is required");
        }
        if (reorderPoint == null) {
            throw new IllegalArgumentException("Reorder point is required");
        }
        if (inventoryPolicy == null) {
            throw new IllegalArgumentException("Inventory policy is required");
        }
    }

    // Getters
    public InventoryId getId() { return id; }
    public ProductId getProductId() { return productId; }
    public StockLevel getCurrentStock() { return currentStock; }
    public StockLevel getReservedStock() { return reservedStock; }
    public StockLevel getAvailableStock() { return availableStock; }
    public ReorderPoint getReorderPoint() { return reorderPoint; }
    public List<StockReservation> getActiveReservations() { return List.copyOf(activeReservations); }
    public List<StockMovement> getMovements() { return List.copyOf(movements); }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public List<DomainEvent> getDomainEvents() { return List.copyOf(domainEvents); }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    // Result classes for complex operations
    public static class ReservationResult {
        private final boolean successful;
        private final ReservationId reservationId;
        private final LocalDateTime expiresAt;
        private final String failureReason;

        private ReservationResult(boolean successful, ReservationId reservationId,
                                LocalDateTime expiresAt, String failureReason) {
            this.successful = successful;
            this.reservationId = reservationId;
            this.expiresAt = expiresAt;
            this.failureReason = failureReason;
        }

        public static ReservationResult successful(ReservationId reservationId, LocalDateTime expiresAt) {
            return new ReservationResult(true, reservationId, expiresAt, null);
        }

        public static ReservationResult failed(String reason) {
            return new ReservationResult(false, null, null, reason);
        }

        public boolean isSuccessful() { return successful; }
        public ReservationId getReservationId() { return reservationId; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public String getFailureReason() { return failureReason; }
    }

    public static class AvailabilityResult {
        private final AvailabilityStatus status;
        private final Quantity availableQuantity;
        private final Quantity shortageQuantity;
        private final LocalDateTime availableDate;

        private AvailabilityResult(AvailabilityStatus status, Quantity availableQuantity,
                                 Quantity shortageQuantity, LocalDateTime availableDate) {
            this.status = status;
            this.availableQuantity = availableQuantity;
            this.shortageQuantity = shortageQuantity;
            this.availableDate = availableDate;
        }

        public static AvailabilityResult available(Quantity quantity) {
            return new AvailabilityResult(AvailabilityStatus.AVAILABLE, quantity, null, null);
        }

        public static AvailabilityResult availableByDate(Quantity quantity, LocalDateTime date) {
            return new AvailabilityResult(AvailabilityStatus.AVAILABLE_BY_DATE, quantity, null, date);
        }

        public static AvailabilityResult insufficient(Quantity available, Quantity shortage) {
            return new AvailabilityResult(AvailabilityStatus.INSUFFICIENT, available, shortage, null);
        }

        public AvailabilityStatus getStatus() { return status; }
        public Quantity getAvailableQuantity() { return availableQuantity; }
        public Quantity getShortageQuantity() { return shortageQuantity; }
        public LocalDateTime getAvailableDate() { return availableDate; }
    }

    public enum AvailabilityStatus {
        AVAILABLE, AVAILABLE_BY_DATE, INSUFFICIENT
    }
}
```

### Stock Reservation Entity

```java
// domain/model/StockReservation.java
package com.example.inventory.domain.model;

import com.example.inventory.domain.valueobject.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Stock Reservation Entity
 *
 * Represents a temporary allocation of stock for a specific order.
 * Has expiration time after which stock is automatically released.
 */
public class StockReservation {
    private ReservationId id;
    private InventoryId inventoryId;
    private ProductId productId;
    private OrderId orderId;
    private Quantity quantity;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private ReservationStatus status;

    private StockReservation() {}

    public static StockReservation create(
        ReservationId id,
        InventoryId inventoryId,
        ProductId productId,
        OrderId orderId,
        Quantity quantity,
        LocalDateTime expiresAt
    ) {
        validateCreation(id, inventoryId, productId, orderId, quantity, expiresAt);

        StockReservation reservation = new StockReservation();
        reservation.id = id;
        reservation.inventoryId = inventoryId;
        reservation.productId = productId;
        reservation.orderId = orderId;
        reservation.quantity = quantity;
        reservation.createdAt = LocalDateTime.now();
        reservation.expiresAt = expiresAt;
        reservation.status = ReservationStatus.ACTIVE;

        return reservation;
    }

    /**
     * Check if reservation is expired
     */
    public boolean isExpired(LocalDateTime checkTime) {
        return checkTime.isAfter(expiresAt);
    }

    /**
     * Check if reservation is close to expiration
     */
    public boolean isNearExpiration(LocalDateTime checkTime, Duration warningPeriod) {
        LocalDateTime warningTime = expiresAt.minus(warningPeriod);
        return checkTime.isAfter(warningTime) && !isExpired(checkTime);
    }

    /**
     * Extend reservation expiration
     */
    public StockReservation extend(Duration additionalTime) {
        if (this.status != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Can only extend active reservations");
        }

        StockReservation extended = new StockReservation();
        extended.id = this.id;
        extended.inventoryId = this.inventoryId;
        extended.productId = this.productId;
        extended.orderId = this.orderId;
        extended.quantity = this.quantity;
        extended.createdAt = this.createdAt;
        extended.expiresAt = this.expiresAt.plus(additionalTime);
        extended.status = ReservationStatus.ACTIVE;

        return extended;
    }

    /**
     * Mark reservation as confirmed (allocated)
     */
    public void confirm() {
        if (this.status != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Can only confirm active reservations");
        }
        this.status = ReservationStatus.CONFIRMED;
    }

    /**
     * Mark reservation as cancelled
     */
    public void cancel() {
        if (this.status != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Can only cancel active reservations");
        }
        this.status = ReservationStatus.CANCELLED;
    }

    /**
     * Mark reservation as expired
     */
    public void markExpired() {
        if (this.status != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Can only expire active reservations");
        }
        this.status = ReservationStatus.EXPIRED;
    }

    private static void validateCreation(
        ReservationId id,
        InventoryId inventoryId,
        ProductId productId,
        OrderId orderId,
        Quantity quantity,
        LocalDateTime expiresAt
    ) {
        if (id == null) {
            throw new IllegalArgumentException("Reservation ID is required");
        }
        if (inventoryId == null) {
            throw new IllegalArgumentException("Inventory ID is required");
        }
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (quantity == null || quantity.isNegativeOrZero()) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Expiration time must be in the future");
        }
    }

    // Getters
    public ReservationId getId() { return id; }
    public InventoryId getInventoryId() { return inventoryId; }
    public ProductId getProductId() { return productId; }
    public OrderId getOrderId() { return orderId; }
    public Quantity getQuantity() { return quantity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public ReservationStatus getStatus() { return status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockReservation that = (StockReservation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public enum ReservationStatus {
        ACTIVE,      // Reservation is active and holding stock
        CONFIRMED,   // Reservation confirmed and converted to allocation
        CANCELLED,   // Reservation cancelled and stock released
        EXPIRED      // Reservation expired and stock automatically released
    }
}
```

### Stock Movement Value Object

```java
// domain/model/StockMovement.java
package com.example.inventory.domain.model;

import com.example.inventory.domain.valueobject.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Stock Movement Value Object
 *
 * Immutable record of stock changes for audit trail.
 * Tracks all inventory movements with reasons and references.
 */
public class StockMovement {
    private final MovementId id;
    private final InventoryId inventoryId;
    private final MovementType movementType;
    private final StockLevel quantity;
    private final String reason;
    private final String reference;
    private final LocalDateTime occurredAt;
    private final StockLevel balanceAfter;

    private StockMovement(
        MovementId id,
        InventoryId inventoryId,
        MovementType movementType,
        StockLevel quantity,
        String reason,
        String reference,
        LocalDateTime occurredAt,
        StockLevel balanceAfter
    ) {
        this.id = id;
        this.inventoryId = inventoryId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.reason = reason;
        this.reference = reference;
        this.occurredAt = occurredAt;
        this.balanceAfter = balanceAfter;
    }

    public static StockMovement initialStock(
        InventoryId inventoryId,
        StockLevel quantity,
        String reason,
        LocalDateTime occurredAt
    ) {
        return new StockMovement(
            MovementId.generate(),
            inventoryId,
            MovementType.INITIAL_STOCK,
            quantity,
            reason,
            "INIT",
            occurredAt,
            quantity
        );
    }

    public static StockMovement stockIncrease(
        InventoryId inventoryId,
        StockLevel quantity,
        String reason,
        String reference,
        LocalDateTime occurredAt
    ) {
        return new StockMovement(
            MovementId.generate(),
            inventoryId,
            MovementType.STOCK_IN,
            quantity,
            reason,
            reference,
            occurredAt,
            null // Balance calculated by aggregate
        );
    }

    public static StockMovement stockDecrease(
        InventoryId inventoryId,
        StockLevel quantity,
        String reason,
        String reference,
        LocalDateTime occurredAt
    ) {
        return new StockMovement(
            MovementId.generate(),
            inventoryId,
            MovementType.STOCK_OUT,
            quantity,
            reason,
            reference,
            occurredAt,
            null
        );
    }

    public static StockMovement reservation(
        InventoryId inventoryId,
        StockLevel quantity,
        String orderReference,
        LocalDateTime occurredAt
    ) {
        return new StockMovement(
            MovementId.generate(),
            inventoryId,
            MovementType.RESERVATION,
            quantity,
            "Stock reserved for order",
            orderReference,
            occurredAt,
            null
        );
    }

    public static StockMovement allocation(
        InventoryId inventoryId,
        StockLevel quantity,
        String orderReference,
        LocalDateTime occurredAt
    ) {
        return new StockMovement(
            MovementId.generate(),
            inventoryId,
            MovementType.ALLOCATION,
            quantity,
            "Stock allocated to order",
            orderReference,
            occurredAt,
            null
        );
    }

    public static StockMovement reservationCancellation(
        InventoryId inventoryId,
        StockLevel quantity,
        String reason,
        LocalDateTime occurredAt
    ) {
        return new StockMovement(
            MovementId.generate(),
            inventoryId,
            MovementType.RESERVATION_RELEASE,
            quantity,
            reason,
            "CANCELLED",
            occurredAt,
            null
        );
    }

    public static StockMovement expiredReservation(
        InventoryId inventoryId,
        StockLevel quantity,
        String reservationReference,
        LocalDateTime occurredAt
    ) {
        return new StockMovement(
            MovementId.generate(),
            inventoryId,
            MovementType.RESERVATION_EXPIRED,
            quantity,
            "Reservation expired",
            reservationReference,
            occurredAt,
            null
        );
    }

    public StockMovement withBalanceAfter(StockLevel balance) {
        return new StockMovement(
            this.id,
            this.inventoryId,
            this.movementType,
            this.quantity,
            this.reason,
            this.reference,
            this.occurredAt,
            balance
        );
    }

    // Getters
    public MovementId getId() { return id; }
    public InventoryId getInventoryId() { return inventoryId; }
    public MovementType getMovementType() { return movementType; }
    public StockLevel getQuantity() { return quantity; }
    public String getReason() { return reason; }
    public String getReference() { return reference; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public StockLevel getBalanceAfter() { return balanceAfter; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockMovement that = (StockMovement) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public enum MovementType {
        INITIAL_STOCK,          // Initial inventory setup
        STOCK_IN,              // Stock received (purchase, return, etc.)
        STOCK_OUT,             // Stock removed (damage, theft, etc.)
        RESERVATION,           // Stock reserved for order
        ALLOCATION,            // Reserved stock allocated to order
        RESERVATION_RELEASE,   // Reservation cancelled
        RESERVATION_EXPIRED,   // Reservation expired
        ADJUSTMENT             // Manual adjustment
    }
}
```

## Inventory Domain Service

```java
// domain/service/InventoryDomainService.java
package com.example.inventory.domain.service;

import com.example.inventory.domain.model.Inventory;
import com.example.inventory.domain.valueobject.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Inventory Domain Service
 *
 * Handles complex business logic that spans multiple aggregates
 * or involves coordination with external systems.
 */
public class InventoryDomainService {

    /**
     * Allocate stock across multiple products for an order
     */
    public MultiProductAllocationResult allocateMultipleProducts(
        List<ProductAllocationRequest> requests,
        OrderId orderId,
        Duration reservationDuration
    ) {
        List<String> failures = new ArrayList<>();
        List<ReservationRecord> successfulReservations = new ArrayList<>();

        for (ProductAllocationRequest request : requests) {
            Inventory inventory = request.getInventory();

            Inventory.ReservationResult result = inventory.reserveStock(
                request.getQuantity(),
                orderId,
                reservationDuration
            );

            if (result.isSuccessful()) {
                successfulReservations.add(new ReservationRecord(
                    request.getProductId(),
                    result.getReservationId(),
                    request.getQuantity(),
                    result.getExpiresAt()
                ));
            } else {
                failures.add(String.format(
                    "Product %s: %s",
                    request.getProductId().getValue(),
                    result.getFailureReason()
                ));
            }
        }

        return new MultiProductAllocationResult(successfulReservations, failures);
    }

    /**
     * Calculate optimal reorder quantities based on demand patterns
     */
    public ReorderRecommendation calculateReorderRecommendation(
        Inventory inventory,
        DemandForecast demandForecast,
        SupplierLeadTime leadTime
    ) {
        Quantity currentStock = inventory.getAvailableStock().getQuantity();
        Quantity reorderPoint = inventory.getReorderPoint().getQuantity();

        // Economic Order Quantity calculation
        Quantity averageDemand = demandForecast.getAverageMonthlyDemand();
        Quantity safetyStock = calculateSafetyStock(demandForecast, leadTime);

        // Calculate optimal order quantity
        Quantity economicOrderQuantity = calculateEOQ(
            averageDemand,
            demandForecast.getOrderingCost(),
            demandForecast.getHoldingCost()
        );

        // Determine if reorder is needed
        boolean shouldReorder = currentStock.isLessThanOrEqual(reorderPoint);

        Quantity recommendedQuantity = shouldReorder
            ? economicOrderQuantity.add(safetyStock).subtract(currentStock)
            : Quantity.ZERO;

        return new ReorderRecommendation(
            inventory.getProductId(),
            shouldReorder,
            recommendedQuantity,
            calculateExpectedStockoutDate(inventory, demandForecast),
            Priority.fromStockLevel(currentStock, reorderPoint)
        );
    }

    /**
     * Assess inventory risk across multiple factors
     */
    public InventoryRiskAssessment assessInventoryRisk(
        Inventory inventory,
        DemandVolatility demandVolatility,
        SupplierReliability supplierReliability,
        SeasonalityFactor seasonality
    ) {
        List<RiskFactor> riskFactors = new ArrayList<>();
        int totalRiskScore = 0;

        // Stock level risk
        Quantity currentStock = inventory.getAvailableStock().getQuantity();
        Quantity reorderPoint = inventory.getReorderPoint().getQuantity();

        if (currentStock.isLessThan(reorderPoint)) {
            riskFactors.add(RiskFactor.LOW_STOCK);
            totalRiskScore += 30;
        }

        // Demand volatility risk
        if (demandVolatility.isHigh()) {
            riskFactors.add(RiskFactor.HIGH_DEMAND_VOLATILITY);
            totalRiskScore += 25;
        }

        // Supplier reliability risk
        if (supplierReliability.isLow()) {
            riskFactors.add(RiskFactor.UNRELIABLE_SUPPLIER);
            totalRiskScore += 35;
        }

        // Seasonal risk
        if (seasonality.isHighSeason() && currentStock.isLessThan(seasonality.getRecommendedStock())) {
            riskFactors.add(RiskFactor.SEASONAL_DEMAND_SPIKE);
            totalRiskScore += 20;
        }

        // Over-reservation risk
        if (hasHighReservationRatio(inventory)) {
            riskFactors.add(RiskFactor.HIGH_RESERVATION_RATIO);
            totalRiskScore += 15;
        }

        RiskLevel riskLevel = RiskLevel.fromScore(totalRiskScore);

        return new InventoryRiskAssessment(
            inventory.getProductId(),
            riskLevel,
            totalRiskScore,
            riskFactors,
            generateRiskMitigationActions(riskFactors)
        );
    }

    /**
     * Optimize stock distribution across multiple warehouses
     */
    public StockDistributionPlan optimizeStockDistribution(
        List<WarehouseInventory> warehouses,
        DemandDistribution demandByRegion,
        TransferCosts transferCosts
    ) {
        List<StockTransfer> recommendations = new ArrayList<>();

        for (WarehouseInventory source : warehouses) {
            if (source.hasExcessStock()) {
                for (WarehouseInventory target : warehouses) {
                    if (target.hasStockShortage() &&
                        transferCosts.isEconomicallyViable(source.getLocation(), target.getLocation())) {

                        Quantity transferQuantity = calculateOptimalTransferQuantity(
                            source, target, transferCosts
                        );

                        if (transferQuantity.isPositive()) {
                            recommendations.add(new StockTransfer(
                                source.getLocation(),
                                target.getLocation(),
                                source.getProductId(),
                                transferQuantity,
                                transferCosts.calculateCost(source.getLocation(), target.getLocation(), transferQuantity)
                            ));
                        }
                    }
                }
            }
        }

        return new StockDistributionPlan(recommendations, calculateTotalCost(recommendations));
    }

    private Quantity calculateSafetyStock(DemandForecast forecast, SupplierLeadTime leadTime) {
        // Safety stock = Z * sqrt(lead time) * demand standard deviation
        double zScore = 1.65; // 95% service level
        double leadTimeDays = leadTime.getDays();
        double demandStdDev = forecast.getStandardDeviation();

        double safetyStockValue = zScore * Math.sqrt(leadTimeDays) * demandStdDev;
        return Quantity.of((int) Math.ceil(safetyStockValue));
    }

    private Quantity calculateEOQ(Quantity averageDemand, Money orderingCost, Money holdingCost) {
        // EOQ = sqrt((2 * D * S) / H)
        double annualDemand = averageDemand.getValue() * 12.0;
        double orderingCostValue = orderingCost.getAmount().doubleValue();
        double holdingCostValue = holdingCost.getAmount().doubleValue();

        double eoq = Math.sqrt((2 * annualDemand * orderingCostValue) / holdingCostValue);
        return Quantity.of((int) Math.ceil(eoq));
    }

    private LocalDateTime calculateExpectedStockoutDate(Inventory inventory, DemandForecast forecast) {
        Quantity currentStock = inventory.getAvailableStock().getQuantity();
        Quantity dailyDemand = forecast.getAverageDailyDemand();

        if (dailyDemand.isZero()) {
            return null; // No stockout expected
        }

        int daysUntilStockout = currentStock.getValue() / dailyDemand.getValue();
        return LocalDateTime.now().plusDays(daysUntilStockout);
    }

    private boolean hasHighReservationRatio(Inventory inventory) {
        Quantity totalStock = inventory.getCurrentStock().getQuantity();
        Quantity reservedStock = inventory.getReservedStock().getQuantity();

        if (totalStock.isZero()) {
            return false;
        }

        double reservationRatio = (double) reservedStock.getValue() / totalStock.getValue();
        return reservationRatio > 0.8; // 80% threshold
    }

    private List<String> generateRiskMitigationActions(List<RiskFactor> riskFactors) {
        List<String> actions = new ArrayList<>();

        for (RiskFactor factor : riskFactors) {
            switch (factor) {
                case LOW_STOCK -> actions.add("Expedite reorder process");
                case HIGH_DEMAND_VOLATILITY -> actions.add("Increase safety stock levels");
                case UNRELIABLE_SUPPLIER -> actions.add("Identify alternative suppliers");
                case SEASONAL_DEMAND_SPIKE -> actions.add("Build seasonal inventory buffer");
                case HIGH_RESERVATION_RATIO -> actions.add("Review reservation policies");
            }
        }

        return actions;
    }

    private Quantity calculateOptimalTransferQuantity(
        WarehouseInventory source,
        WarehouseInventory target,
        TransferCosts costs
    ) {
        Quantity sourceExcess = source.getExcessQuantity();
        Quantity targetShortage = target.getShortageQuantity();

        return sourceExcess.isLessThan(targetShortage) ? sourceExcess : targetShortage;
    }

    private Money calculateTotalCost(List<StockTransfer> transfers) {
        return transfers.stream()
            .map(StockTransfer::getCost)
            .reduce(Money.ZERO, Money::add);
    }

    // Result classes and supporting types would be defined here...
}
```
