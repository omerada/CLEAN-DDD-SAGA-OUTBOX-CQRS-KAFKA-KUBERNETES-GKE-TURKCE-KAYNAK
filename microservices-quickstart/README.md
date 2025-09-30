# Mikroservisler QuickStart Guide

## Proje Yapısı

```
microservices-quickstart/
├── docker-compose.yml
├── order-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/example/order/
├── inventory-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/example/inventory/
├── payment-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/example/payment/
└── scripts/
    ├── start.sh
    └── smoke-test.sh
```

## Hızlı Başlangıç

### 1. Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose

### 2. Build & Run

```bash
# Servisleri build et
./scripts/start.sh

# Smoke test çalıştır
./scripts/smoke-test.sh
```

### 3. Test Endpoints

- Order Service: http://localhost:8081/orders
- Inventory Service: http://localhost:8082/inventory
- Payment Service: http://localhost:8083/payments

### 4. Database Access

- PostgreSQL: localhost:5432
- pgAdmin: http://localhost:5050 (admin@admin.com / admin)
