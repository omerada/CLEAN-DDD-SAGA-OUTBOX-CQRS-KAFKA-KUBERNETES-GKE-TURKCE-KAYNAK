package com.example.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Inventory Service - Stok yönetimi mikroservisi
 * 
 * Bu servis şu sorumlulukları üstlenir:
 * - Ürün stok durumlarını yönetme
 * - Stok rezervasyonu ve serbest bırakma
 * - OrderCreated event'ini consume edip otomatik rezervasyon
 * - InventoryReserved, InventoryReleased event'lerini publish etme
 */
@SpringBootApplication
@EnableKafka
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
        System.out.println("📦 Inventory Service started successfully!");
        System.out.println("📋 API Documentation: http://localhost:8082/actuator/health");
        System.out.println("🔗 Endpoints: http://localhost:8082/inventory");
    }
}