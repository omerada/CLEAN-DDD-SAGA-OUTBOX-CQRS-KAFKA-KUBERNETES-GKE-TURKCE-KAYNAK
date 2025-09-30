package com.example.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Order Service - E-ticaret sipariş yönetimi mikroservisi
 * 
 * Bu servis şu sorumlulukları üstlenir:
 * - Sipariş oluşturma, güncelleme, listeleme
 * - Sipariş durumu yönetimi (PENDING, CONFIRMED, CANCELLED)
 * - OrderCreated, OrderConfirmed event'lerini Kafka'ya publish etme
 * - Payment ve Inventory servislerinden gelen event'leri consume etme
 */
@SpringBootApplication
@EnableKafka
@EnableScheduling
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
        System.out.println("🚀 Order Service started successfully!");
        System.out.println("📋 API Documentation: http://localhost:8081/actuator/health");
        System.out.println("🔗 Endpoints: http://localhost:8081/orders");
    }
}