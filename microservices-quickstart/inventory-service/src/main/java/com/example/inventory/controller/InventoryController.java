package com.example.inventory.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Inventory Controller - Stok yönetimi API endpoint'leri
 * 
 * Bu controller QuickStart Lab için minimal implementation içerir:
 * - GET /inventory/{productId} - Ürün stok durumunu sorgula
 * - POST /inventory/reserve - Stok rezervasyonu yap
 * - POST /inventory/release - Stok rezervasyonunu serbest bırak
 * - GET /inventory - Tüm ürün stoklarını listele
 */
@RestController
@RequestMapping("/inventory")
@CrossOrigin(origins = "*")
public class InventoryController {

    // In-memory storage for QuickStart (production'da JPA Repository kullanılacak)
    private final Map<String, InventoryItem> inventory = new HashMap<>();

    public InventoryController() {
        // Sample data initialization
        initializeSampleData();
    }

    /**
     * Ürün stok durumunu sorgula
     */
    @GetMapping("/{productId}")
    public ResponseEntity<InventoryItem> getInventoryByProductId(@PathVariable String productId) {
        System.out.println("📦 Checking inventory for product: " + productId);
        InventoryItem item = inventory.get(productId);
        if (item != null) {
            return ResponseEntity.ok(item);
        }

        // Ürün bulunamazsa default stok oluştur
        InventoryItem newItem = new InventoryItem();
        newItem.setProductId(productId);
        newItem.setAvailableQuantity(100); // Default stock
        newItem.setReservedQuantity(0);
        newItem.setLastUpdated(LocalDateTime.now());
        inventory.put(productId, newItem);

        return ResponseEntity.ok(newItem);
    }

    /**
     * Stok rezervasyonu yap
     */
    @PostMapping("/reserve")
    public ResponseEntity<ReservationResponse> reserveInventory(@RequestBody ReserveInventoryRequest request) {
        System.out.println("🔒 Reserving inventory for product: " + request.getProductId() +
                ", quantity: " + request.getQuantity());

        InventoryItem item = inventory.get(request.getProductId());
        if (item == null) {
            return ResponseEntity.badRequest().body(
                    new ReservationResponse(false, "Product not found", null));
        }

        if (item.getAvailableQuantity() >= request.getQuantity()) {
            item.setAvailableQuantity(item.getAvailableQuantity() - request.getQuantity());
            item.setReservedQuantity(item.getReservedQuantity() + request.getQuantity());
            item.setLastUpdated(LocalDateTime.now());

            // TODO: Kafka'ya InventoryReserved event publish et
            System.out.println("📤 InventoryReserved event should be published to Kafka");

            return ResponseEntity.ok(new ReservationResponse(true, "Inventory reserved successfully", item));
        } else {
            return ResponseEntity.badRequest().body(
                    new ReservationResponse(false, "Insufficient inventory", item));
        }
    }

    /**
     * Stok rezervasyonunu serbest bırak
     */
    @PostMapping("/release")
    public ResponseEntity<ReservationResponse> releaseInventory(@RequestBody ReleaseInventoryRequest request) {
        System.out.println("🔓 Releasing inventory for product: " + request.getProductId() +
                ", quantity: " + request.getQuantity());

        InventoryItem item = inventory.get(request.getProductId());
        if (item == null) {
            return ResponseEntity.badRequest().body(
                    new ReservationResponse(false, "Product not found", null));
        }

        if (item.getReservedQuantity() >= request.getQuantity()) {
            item.setReservedQuantity(item.getReservedQuantity() - request.getQuantity());
            item.setAvailableQuantity(item.getAvailableQuantity() + request.getQuantity());
            item.setLastUpdated(LocalDateTime.now());

            // TODO: Kafka'ya InventoryReleased event publish et
            System.out.println("📤 InventoryReleased event should be published to Kafka");

            return ResponseEntity.ok(new ReservationResponse(true, "Inventory released successfully", item));
        } else {
            return ResponseEntity.badRequest().body(
                    new ReservationResponse(false, "Invalid release quantity", item));
        }
    }

    /**
     * Tüm ürün stoklarını listele
     */
    @GetMapping
    public ResponseEntity<List<InventoryItem>> getAllInventory() {
        System.out.println("📋 Listing all inventory items, count: " + inventory.size());
        return ResponseEntity.ok(new ArrayList<>(inventory.values()));
    }

    private void initializeSampleData() {
        // Sample products with initial stock
        String[] products = { "PROD-123", "PROD-456", "PROD-789" };
        for (String productId : products) {
            InventoryItem item = new InventoryItem();
            item.setProductId(productId);
            item.setAvailableQuantity(100);
            item.setReservedQuantity(0);
            item.setLastUpdated(LocalDateTime.now());
            inventory.put(productId, item);
        }
        System.out.println("📦 Initialized sample inventory data for " + products.length + " products");
    }

    // Inner classes for QuickStart
    public static class InventoryItem {
        private String productId;
        private Integer availableQuantity;
        private Integer reservedQuantity;
        private LocalDateTime lastUpdated;

        // Getters and Setters
        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public Integer getAvailableQuantity() {
            return availableQuantity;
        }

        public void setAvailableQuantity(Integer availableQuantity) {
            this.availableQuantity = availableQuantity;
        }

        public Integer getReservedQuantity() {
            return reservedQuantity;
        }

        public void setReservedQuantity(Integer reservedQuantity) {
            this.reservedQuantity = reservedQuantity;
        }

        public LocalDateTime getLastUpdated() {
            return lastUpdated;
        }

        public void setLastUpdated(LocalDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
        }
    }

    public static class ReserveInventoryRequest {
        private String productId;
        private Integer quantity;
        private String orderId;

        // Getters and Setters
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

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }
    }

    public static class ReleaseInventoryRequest {
        private String productId;
        private Integer quantity;
        private String orderId;

        // Getters and Setters
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

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }
    }

    public static class ReservationResponse {
        private boolean success;
        private String message;
        private InventoryItem inventoryItem;

        public ReservationResponse(boolean success, String message, InventoryItem inventoryItem) {
            this.success = success;
            this.message = message;
            this.inventoryItem = inventoryItem;
        }

        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public InventoryItem getInventoryItem() {
            return inventoryItem;
        }

        public void setInventoryItem(InventoryItem inventoryItem) {
            this.inventoryItem = inventoryItem;
        }
    }
}