package com.example.order.application.query.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Order List View - Read Model
 * 
 * CQRS Read Side optimized model for order list display.
 * Denormalized for fast querying and UI consumption.
 */
@Entity
@Table(name = "order_list_view", indexes = {
    @Index(name = "idx_order_list_customer", columnList = "customerId"),
    @Index(name = "idx_order_list_status", columnList = "status"),
    @Index(name = "idx_order_list_created", columnList = "createdAt"),
    @Index(name = "idx_order_list_customer_status", columnList = "customerId, status")
})
public class OrderListView {
    
    @Id
    @Column(name = "order_id")
    private String orderId;
    
    @Column(name = "customer_id", nullable = false)
    private String customerId;
    
    @Column(name = "status", nullable = false)
    private String status;
    
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "currency", nullable = false)
    private String currency;
    
    @Column(name = "item_count", nullable = false)
    private Integer itemCount;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    protected OrderListView() {} // JPA constructor
    
    public OrderListView(
        String orderId,
        String customerId,
        String status,
        BigDecimal totalAmount,
        String currency,
        Integer itemCount,
        LocalDateTime createdAt
    ) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.itemCount = itemCount;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }
    
    /**
     * Update order status
     */
    public void updateStatus(String newStatus, LocalDateTime updatedAt) {
        this.status = newStatus;
        this.updatedAt = updatedAt;
    }
    
    /**
     * Factory method for creating from domain order
     */
    public static OrderListView fromDomainOrder(
        String orderId,
        String customerId,
        String status,
        BigDecimal totalAmount,
        String currency,
        Integer itemCount,
        LocalDateTime createdAt
    ) {
        return new OrderListView(
            orderId,
            customerId,
            status,
            totalAmount,
            currency,
            itemCount,
            createdAt
        );
    }
    
    // Getters
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public Integer getItemCount() { return itemCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    
    /**
     * Business methods
     */
    public boolean isActive() {
        return !"CANCELLED".equals(status) && !"DELIVERED".equals(status);
    }
    
    public boolean isPending() {
        return "PENDING".equals(status);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OrderListView that = (OrderListView) obj;
        return Objects.equals(orderId, that.orderId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }
    
    @Override
    public String toString() {
        return "OrderListView{" +
                "orderId='" + orderId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", status='" + status + '\'' +
                ", totalAmount=" + totalAmount +
                ", itemCount=" + itemCount +
                '}';
    }
}