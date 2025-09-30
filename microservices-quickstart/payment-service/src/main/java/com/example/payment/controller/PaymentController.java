package com.example.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Payment Controller - Ödeme işlemleri API endpoint'leri
 * 
 * Bu controller QuickStart Lab için minimal implementation içerir:
 * - POST /payments - Yeni ödeme işlemi başlat
 * - GET /payments/{id} - Ödeme detayını getir
 * - GET /payments/order/{orderId} - Sipariş ödemelerini getir
 * - GET /payments - Tüm ödemeleri listele
 */
@RestController
@RequestMapping("/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    // In-memory storage for QuickStart (production'da JPA Repository kullanılacak)
    private final Map<Long, Payment> payments = new HashMap<>();
    private Long idCounter = 1L;

    /**
     * Yeni ödeme işlemi başlat
     */
    @PostMapping
    public ResponseEntity<Payment> processPayment(@RequestBody ProcessPaymentRequest request) {
        System.out.println("💳 Processing payment for order: " + request.getOrderId() +
                ", amount: $" + request.getAmount());

        Payment payment = new Payment();
        payment.setId(idCounter++);
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setCardToken(request.getCardToken());
        payment.setStatus("PROCESSING");
        payment.setCreatedAt(LocalDateTime.now());

        // Simulate payment processing logic
        boolean paymentSuccess = simulatePaymentGateway(request);

        if (paymentSuccess) {
            payment.setStatus("COMPLETED");
            payment.setProcessedAt(LocalDateTime.now());
            System.out.println("✅ Payment completed successfully for order: " + request.getOrderId());

            // TODO: Kafka'ya PaymentCompleted event publish et
            System.out.println("📤 PaymentCompleted event should be published to Kafka");
        } else {
            payment.setStatus("FAILED");
            payment.setFailureReason("Payment gateway declined the transaction");
            System.out.println("❌ Payment failed for order: " + request.getOrderId());

            // TODO: Kafka'ya PaymentFailed event publish et
            System.out.println("📤 PaymentFailed event should be published to Kafka");
        }

        payments.put(payment.getId(), payment);
        return ResponseEntity.ok(payment);
    }

    /**
     * Ödeme detayını getir
     */
    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long id) {
        System.out.println("🔍 Fetching payment with id: " + id);
        Payment payment = payments.get(id);
        if (payment != null) {
            return ResponseEntity.ok(payment);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Sipariş ödemelerini getir
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Payment>> getPaymentsByOrderId(@PathVariable String orderId) {
        System.out.println("📋 Fetching payments for order: " + orderId);
        List<Payment> orderPayments = payments.values().stream()
                .filter(payment -> payment.getOrderId().equals(orderId))
                .toList();
        return ResponseEntity.ok(orderPayments);
    }

    /**
     * Tüm ödemeleri listele
     */
    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        System.out.println("📋 Listing all payments, count: " + payments.size());
        return ResponseEntity.ok(new ArrayList<>(payments.values()));
    }

    /**
     * Ödeme iade işlemi
     */
    @PostMapping("/{id}/refund")
    public ResponseEntity<Payment> refundPayment(@PathVariable Long id) {
        System.out.println("💸 Processing refund for payment: " + id);

        Payment payment = payments.get(id);
        if (payment == null) {
            return ResponseEntity.notFound().build();
        }

        if (!"COMPLETED".equals(payment.getStatus())) {
            return ResponseEntity.badRequest().build();
        }

        payment.setStatus("REFUNDED");
        payment.setRefundedAt(LocalDateTime.now());

        // TODO: Kafka'ya PaymentRefunded event publish et
        System.out.println("📤 PaymentRefunded event should be published to Kafka");

        return ResponseEntity.ok(payment);
    }

    /**
     * Payment Gateway simulation
     * %90 success rate for demo purposes
     */
    private boolean simulatePaymentGateway(ProcessPaymentRequest request) {
        // Simulate network delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate 90% success rate
        return Math.random() > 0.1;
    }

    // Inner classes for QuickStart
    public static class Payment {
        private Long id;
        private String orderId;
        private Double amount;
        private String paymentMethod;
        private String cardToken;
        private String status;
        private String failureReason;
        private LocalDateTime createdAt;
        private LocalDateTime processedAt;
        private LocalDateTime refundedAt;

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public String getCardToken() {
            return cardToken;
        }

        public void setCardToken(String cardToken) {
            this.cardToken = cardToken;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public void setFailureReason(String failureReason) {
            this.failureReason = failureReason;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getProcessedAt() {
            return processedAt;
        }

        public void setProcessedAt(LocalDateTime processedAt) {
            this.processedAt = processedAt;
        }

        public LocalDateTime getRefundedAt() {
            return refundedAt;
        }

        public void setRefundedAt(LocalDateTime refundedAt) {
            this.refundedAt = refundedAt;
        }
    }

    public static class ProcessPaymentRequest {
        private String orderId;
        private Double amount;
        private String paymentMethod; // CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER
        private String cardToken;

        // Getters and Setters
        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public String getCardToken() {
            return cardToken;
        }

        public void setCardToken(String cardToken) {
            this.cardToken = cardToken;
        }
    }
}