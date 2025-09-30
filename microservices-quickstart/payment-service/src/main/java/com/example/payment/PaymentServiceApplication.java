package com.example.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Payment Service - Ödeme işlemleri mikroservisi
 * 
 * Bu servis şu sorumlulukları üstlenir:
 * - Ödeme işlemlerini gerçekleştirme (CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER)
 * - Ödeme durumu takibi (PENDING, COMPLETED, FAILED, REFUNDED)
 * - InventoryReserved event'ini consume edip otomatik ödeme tetikleme
 * - PaymentCompleted, PaymentFailed event'lerini publish etme
 */
@SpringBootApplication
@EnableKafka
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
        System.out.println("💳 Payment Service started successfully!");
        System.out.println("📋 API Documentation: http://localhost:8083/actuator/health");
        System.out.println("🔗 Endpoints: http://localhost:8083/payments");
    }
}