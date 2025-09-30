package com.example.order.domain.entity;

import com.example.order.domain.valueobject.*;
import com.example.order.domain.event.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Order Aggregate Root
 * 
 * Business Rules Enforced:
 * - Order must have at least one item
 * - Total amount must be positive
 * - Status transitions must follow business rules
 * - Items cannot be modified after confirmation
 * - Domain events are published for state changes
 */
public class Order {
    private OrderId id;
    private CustomerId customerId;
    private List<OrderItem> items;
    private OrderStatus status;
    private Money totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Domain events collection
    private List<Object> domainEvents = new ArrayList<>();

    // Private constructor for persistence frameworks
    private Order() {
        this.items = new ArrayList<>();
    }

    private Order(OrderId id, CustomerId customerId, List<OrderItem> items) {
        this();
        this.id = id;
        this.customerId = customerId;
        this.items = new ArrayList<>(items);
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.totalAmount = calculateTotalAmount();
    }

    /**
     * Factory method - Aggregate creation with business rule validation
     */
    public static Order create(CustomerId customerId, List<OrderItem> items) {
        validateOrderCreation(customerId, items);

        OrderId orderId = OrderId.generate();
        Order order = new Order(orderId, customerId, items);

        // Publish domain event
        order.addDomainEvent(new OrderCreatedEvent(
                orderId,
                customerId,
                order.totalAmount,
                order.getTotalItemsCount(),
                order.createdAt));

        return order;
    }

    private static void validateOrderCreation(CustomerId customerId, List<OrderItem> items) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        if (items.size() > 50) {
            throw new IllegalArgumentException("Order cannot contain more than 50 items");
        }
    }

    /**
     * Business Logic: Confirm order (status transition)
     */
    public void confirm() {
        if (!status.canTransitionTo(OrderStatus.CONFIRMED)) {
            throw new IllegalStateException(
                    String.format("Cannot confirm order in %s status", status));
        }

        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = LocalDateTime.now();

        // Publish domain event
        addDomainEvent(new OrderConfirmedEvent(
                this.id,
                this.customerId,
                this.totalAmount,
                this.updatedAt));
    }

    /**
     * Business Logic: Cancel order
     */
    public void cancel(String reason) {
        if (!status.isCancellable()) {
            throw new IllegalStateException(
                    String.format("Cannot cancel order in %s status", status));
        }

        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();

        // Publish domain event
        addDomainEvent(new OrderCancelledEvent(
                this.id,
                this.customerId,
                reason,
                this.updatedAt));
    }

    /**
     * Business Logic: Ship order
     */
    public void ship() {
        if (!status.canTransitionTo(OrderStatus.SHIPPED)) {
            throw new IllegalStateException(
                    String.format("Cannot ship order in %s status", status));
        }

        this.status = OrderStatus.SHIPPED;
        this.updatedAt = LocalDateTime.now();

        // Domain event would be published here
    }

    /**
     * Business Logic: Mark as delivered
     */
    public void markAsDelivered() {
        if (!status.canTransitionTo(OrderStatus.DELIVERED)) {
            throw new IllegalStateException(
                    String.format("Cannot mark order as delivered in %s status", status));
        }

        this.status = OrderStatus.DELIVERED;
        this.updatedAt = LocalDateTime.now();

        // Domain event would be published here
    }

    /**
     * Business Logic: Add item (only if modifiable)
     */
    public void addItem(OrderItem item) {
        if (!status.isModifiable()) {
            throw new IllegalStateException("Cannot modify order in " + status + " status");
        }

        // Check if product already exists
        Optional<OrderItem> existingItem = findItemByProductId(item.getProductId());
        if (existingItem.isPresent()) {
            // Update quantity instead of adding duplicate
            int newQuantity = existingItem.get().getQuantity() + item.getQuantity();
            existingItem.get().updateQuantity(newQuantity);
        } else {
            this.items.add(item);
        }

        this.totalAmount = calculateTotalAmount();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Business Logic: Remove item (only if modifiable)
     */
    public void removeItem(ProductId productId) {
        if (!status.isModifiable()) {
            throw new IllegalStateException("Cannot modify order in " + status + " status");
        }

        boolean removed = items.removeIf(item -> item.getProductId().equals(productId));
        if (!removed) {
            throw new IllegalArgumentException("Item not found in order");
        }

        if (items.isEmpty()) {
            throw new IllegalStateException("Order must contain at least one item");
        }

        this.totalAmount = calculateTotalAmount();
        this.updatedAt = LocalDateTime.now();
    }

    private Optional<OrderItem> findItemByProductId(ProductId productId) {
        return items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();
    }

    private Money calculateTotalAmount() {
        return items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(Money.ZERO, Money::add);
    }

    private int getTotalItemsCount() {
        return items.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }

    // Domain Events Management
    private void addDomainEvent(Object event) {
        this.domainEvents.add(event);
    }

    public List<Object> getDomainEvents() {
        return List.copyOf(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    // Getters (no setters - immutability through behavior)
    public OrderId getId() {
        return id;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public List<OrderItem> getItems() {
        return List.copyOf(items); // Defensive copy
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", status=" + status +
                ", totalAmount=" + totalAmount +
                ", itemsCount=" + items.size() +
                ", createdAt=" + createdAt +
                '}';
    }
}