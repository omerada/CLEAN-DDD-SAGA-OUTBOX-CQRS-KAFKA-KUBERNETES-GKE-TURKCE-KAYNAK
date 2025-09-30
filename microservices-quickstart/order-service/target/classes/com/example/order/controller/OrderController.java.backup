package com.example.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Order Controller - Sipariş API endpoint'leri
 * 
 * Bu controller QuickStart Lab için minimal implementation içerir:
 * - GET /orders - Tüm siparişleri listele
 * - GET /orders/{id} - Sipariş detayını getir
 * - POST /orders - Yeni sipariş oluştur
 * - GET /orders/customer/{customerId} - Müşteri siparişlerini getir
 */
@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    // In-memory storage for QuickStart (production'da JPA Repository kullanılacak)
    private final Map<Long, Order> orders = new HashMap<>();
    private Long idCounter = 1L;

    /**
     * Tüm siparişleri listele
     */
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        System.out.println("📋 Listing all orders, count: " + orders.size());
        return ResponseEntity.ok(new ArrayList<>(orders.values()));
    }

    /**
     * Sipariş detayını getir
     */
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        System.out.println("🔍 Fetching order with id: " + id);
        Order order = orders.get(id);
        if (order != null) {
            return ResponseEntity.ok(order);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Yeni sipariş oluştur
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        System.out.println("✅ Creating new order for customer: " + request.getCustomerId());

        Order order = new Order();
        order.setId(idCounter++);
        order.setCustomerId(request.getCustomerId());
        order.setProductId(request.getProductId());
        order.setQuantity(request.getQuantity());
        order.setPrice(request.getPrice());
        order.setTotalAmount(request.getPrice() * request.getQuantity());
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());

        orders.put(order.getId(), order);

        // TODO: Kafka'ya OrderCreated event publish et
        System.out.println("📤 OrderCreated event should be published to Kafka");

        return ResponseEntity.ok(order);
    }

    /**
     * Müşteri siparişlerini getir
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Order>> getOrdersByCustomer(@PathVariable String customerId) {
        System.out.println("👤 Fetching orders for customer: " + customerId);
        List<Order> customerOrders = orders.values().stream()
                .filter(order -> order.getCustomerId().equals(customerId))
                .toList();
        return ResponseEntity.ok(customerOrders);
    }

    // Inner classes for QuickStart
    public static class Order {
        private Long id;
        private String customerId;
        private String productId;
        private Integer quantity;
        private Double price;
        private Double totalAmount;
        private String status;
        private LocalDateTime createdAt;

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public Double getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(Double totalAmount) {
            this.totalAmount = totalAmount;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class CreateOrderRequest {
        private String customerId;
        private String productId;
        private Integer quantity;
        private Double price;

        // Getters and Setters
        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }
    }
}