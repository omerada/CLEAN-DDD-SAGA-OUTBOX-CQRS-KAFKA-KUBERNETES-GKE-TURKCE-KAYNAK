# 🚀 Mikroservisler Projesi - Production-Ready E-Ticaret Sistemi

Bu proje, **Clean Architecture**, **DDD**, **SAGA**, **Outbox**, **CQRS** pattern'leri ile **Kafka** ve **Kubernetes** kullanarak enterprise-grade mikroservis sistemi geliştirme sürecini öğretir.

## 📋 Proje Özeti

3 ana mikroservisten oluşan e-ticaret domain'i:

- **Order Service** (Port: 8081) - Sipariş yönetimi
- **Inventory Service** (Port: 8082) - Stok yönetimi
- **Payment Service** (Port: 8083) - Ödeme işlemleri

**Infrastructure:**

- PostgreSQL (Port: 5432) - Database per service pattern
- Apache Kafka (Port: 9092) - Event-driven communication
- pgAdmin (Port: 5050) - Database management UI

## 🎯 Öğrenme Hedefleri

Bu proje ile şunları öğreneceksiniz:

### 🏗️ Architecture Patterns

- ✅ **Clean Architecture** - Dependency inversion ve katman separasyonu
- ✅ **Hexagonal Architecture** - Ports & adapters pattern
- ✅ **Domain Driven Design** - Strategic ve tactical patterns
- ✅ **SAGA Pattern** - Distributed transaction management
- ✅ **Outbox Pattern** - Reliable event publishing
- ✅ **CQRS Pattern** - Command/Query responsibility segregation

### ⚡ Technology Stack

- ✅ **Spring Boot 3.x** - Modern Java microservices
- ✅ **Apache Kafka** - Event streaming platform
- ✅ **PostgreSQL** - ACID compliant database
- ✅ **Docker & Docker Compose** - Containerization
- ✅ **Kubernetes** - Container orchestration
- ✅ **Google Cloud GKE** - Production deployment

### 🎓 Advanced Concepts

- ✅ **Event Sourcing** - Temporal data management
- ✅ **Exactly-Once Semantics** - Reliable message processing
- ✅ **Change Data Capture** - Database event streaming
- ✅ **Observability** - Metrics, tracing, logging
- ✅ **Production Readiness** - Security, scaling, monitoring

## 🚀 Hızlı Başlangıç

### Ön Koşullar

```powershell
# 1. Java Development Kit 17+
java -version

# 2. Maven 3.8+
mvn -version

# 3. Docker & Docker Compose
docker --version
docker-compose --version

# 4. PowerShell (Windows) veya Bash (Linux/Mac)
$PSVersionTable.PSVersion  # PowerShell
```

### Sistem Başlatma

```powershell
# 1. Repository clone et
git clone <repo-url>
cd microservices-quickstart

# 2. Tüm sistemi başlat
.\scripts\start.ps1

# 3. Smoke test çalıştır
.\scripts\smoke-test.ps1

# 4. Sistemi durdur
.\scripts\stop.ps1
```

### Manuel Başlatma (Alternatif)

```powershell
# Infrastructure başlat
docker-compose up -d postgres pgadmin zookeeper kafka

# Servisleri build et
mvn clean compile -f order-service/pom.xml
mvn clean compile -f inventory-service/pom.xml
mvn clean compile -f payment-service/pom.xml

# Mikroservisleri başlat
docker-compose up -d order-service inventory-service payment-service
```

## 📡 API Endpoints

### Order Service (Port: 8081)

```bash
# Sipariş oluştur
POST http://localhost:8081/orders
{
  "customerId": "CUST-001",
  "items": [
    {
      "productId": "PROD-123",
      "quantity": 2,
      "unitPrice": 99.99
    }
  ]
}

# Siparişleri listele
GET http://localhost:8081/orders

# Sipariş detayı
GET http://localhost:8081/orders/{orderId}

# Müşteri siparişleri
GET http://localhost:8081/orders/customer/{customerId}
```

### Inventory Service (Port: 8082)

```bash
# Stok durumu sorgula
GET http://localhost:8082/inventory/{productId}

# Stok rezervasyonu
POST http://localhost:8082/inventory/reserve
{
  "productId": "PROD-123",
  "quantity": 2,
  "orderId": "order-123",
  "reservationId": "res-456"
}

# Rezervasyon serbest bırak
POST http://localhost:8082/inventory/release
{
  "productId": "PROD-123",
  "quantity": 2,
  "orderId": "order-123",
  "reservationId": "res-456"
}

# Tüm stokları listele
GET http://localhost:8082/inventory
```

### Payment Service (Port: 8083)

```bash
# Ödeme işlemi
POST http://localhost:8083/payments
{
  "orderId": "order-123",
  "amount": 199.98,
  "paymentMethod": "CREDIT_CARD",
  "cardToken": "tok_123456"
}

# Ödeme durumu
GET http://localhost:8083/payments/{paymentId}

# Sipariş ödemeleri
GET http://localhost:8083/payments/order/{orderId}

# İade işlemi
POST http://localhost:8083/payments/{paymentId}/refund
{
  "amount": 199.98,
  "reason": "Customer request"
}
```

## 🔍 Health Checks

```bash
# Servis durumları
curl http://localhost:8081/actuator/health  # Order Service
curl http://localhost:8082/actuator/health  # Inventory Service
curl http://localhost:8083/actuator/health  # Payment Service

# Database bağlantıları
curl http://localhost:8081/actuator/health/db
curl http://localhost:8082/actuator/health/db
curl http://localhost:8083/actuator/health/db
```

## 🗄️ Database Erişimi

### pgAdmin Web UI

- **URL:** http://localhost:5050
- **Login:** admin@admin.com / admin
- **Server:** postgres:5432
- **Database:** order_db, inventory_db, payment_db

### Direct PostgreSQL

```bash
# Order database
docker exec -it microservices-quickstart-postgres-1 psql -U admin -d order_db

# Inventory database
docker exec -it microservices-quickstart-postgres-1 psql -U admin -d inventory_db

# Payment database
docker exec -it microservices-quickstart-postgres-1 psql -U admin -d payment_db
```

## 📊 Monitoring & Logs

### Container Durumları

```powershell
# Tüm container'ları göster
docker-compose ps

# Spesifik servis logları
docker-compose logs -f order-service
docker-compose logs -f inventory-service
docker-compose logs -f payment-service

# Infrastructure logları
docker-compose logs -f postgres
docker-compose logs -f kafka
```

### Kafka Topics

```bash
# Topic'leri listele
docker exec -it microservices-quickstart-kafka-1 kafka-topics --list --bootstrap-server localhost:9092

# Event'leri consume et
docker exec -it microservices-quickstart-kafka-1 kafka-console-consumer --topic orders --bootstrap-server localhost:9092 --from-beginning
```

## 🧪 Test Scenarios

### Scenario 1: Temel CRUD Operations

```powershell
# PowerShell test script
.\scripts\smoke-test.ps1
```

### Scenario 2: Event-Driven Workflow Test

```bash
# 1. Sipariş oluştur (OrderCreated event publish edilir)
POST http://localhost:8081/orders
{
  "customerId": "CUST-EVENT-001",
  "items": [{"productId": "PROD-123", "quantity": 1, "unitPrice": 99.99}]
}

# 2. Kafka consumer loglarını kontrol et
docker-compose logs inventory-service | grep "OrderCreated"

# 3. Stok durumunu kontrol et (otomatik rezervasyon olmuş olmalı)
GET http://localhost:8082/inventory/PROD-123

# 4. Ödeme işlemi (PaymentCompleted event publish edilir)
POST http://localhost:8083/payments
{
  "orderId": "created-order-id",
  "amount": 99.99,
  "paymentMethod": "CREDIT_CARD"
}

# 5. Final sipariş durumunu kontrol et
GET http://localhost:8081/orders/{orderId}
```

## 🏗️ Mimari Yaklaşım

### Current State (QuickStart)

- ✅ **Layered Architecture** - Controller → Service → Repository
- ✅ **Event-Driven Communication** - Kafka ile async messaging
- ✅ **Database per Service** - PostgreSQL ile data isolation
- ✅ **Containerization** - Docker Compose ile orchestration

### Target State (Advanced Patterns)

- 🎯 **Clean Architecture** - Dependency inversion, use cases
- 🎯 **Hexagonal Architecture** - Ports & adapters
- 🎯 **DDD Implementation** - Aggregates, value objects, domain events
- 🎯 **SAGA Orchestration** - Distributed transaction coordination
- 🎯 **Outbox Pattern** - Reliable event publishing
- 🎯 **CQRS Implementation** - Command/Query segregation
- 🎯 **Event Sourcing** - Kafka as event store
- 🎯 **Kubernetes Deployment** - Production-ready orchestration

## 📚 Öğrenme Yolu

### Temel Seviye (Hafta 1-2)

1. **[QuickStart Lab](../documentation/01-quickstart/)** - Sistem kurulumu ve temel API testleri
2. **[Clean Architecture](../documentation/02-clean-architecture/)** - Katman separasyonu ve dependency inversion
3. **[Hexagonal Architecture](../documentation/03-hexagonal-architecture/)** - Ports & adapters pattern

### Orta Seviye (Hafta 3-5)

4. **[Domain Driven Design](../documentation/04-domain-driven-design/)** - Strategic ve tactical patterns
5. **[SAGA Pattern](../documentation/05-saga-pattern/)** - Distributed transaction management
6. **[Outbox Pattern](../documentation/06-outbox-pattern/)** - Reliable event publishing
7. **[CQRS Pattern](../documentation/07-cqrs-pattern/)** - Command/Query responsibility segregation

### İleri Seviye (Hafta 6-8)

8. **[Kafka Advanced](../documentation/08-kafka-advanced/)** - Advanced programming patterns
9. **[Event Sourcing](../documentation/09-kafka-event-store/)** - Kafka as event store
10. **[Kubernetes](../documentation/10-kubernetes-fundamentals/)** - Container orchestration
11. **[GKE Production](../documentation/11-gke-production/)** - Production deployment

## 🐛 Troubleshooting

### Yaygın Problemler

#### Port Conflict

```powershell
# Port kullanımını kontrol et
netstat -an | findstr :5432
netstat -an | findstr :9092

# Çakışan servisleri durdur
Stop-Service postgresql-x64-13  # Windows
sudo service postgresql stop     # Linux
```

#### Memory Issues

```powershell
# Docker memory kullanımını kontrol et
docker stats

# Kullanılmayan container'ları temizle
docker system prune -a
```

#### Build Failures

```powershell
# Maven cache temizle
mvn dependency:purge-local-repository

# Build dependency'lerini güncelle
mvn clean compile -U
```

### Log Analysis

```bash
# Error logları filtrele
docker-compose logs order-service | grep -i error
docker-compose logs inventory-service | grep -i error
docker-compose logs payment-service | grep -i error

# Kafka connection sorunları
docker-compose logs kafka | grep -i "connection"
```

## 🚀 Production Hazırlığı

### Performance Tuning

- JVM heap size optimization
- Kafka partition configuration
- Database connection pooling
- Caching strategies

### Security Implementation

- JWT token authentication
- Kafka SASL/TLS encryption
- Database connection encryption
- API rate limiting

### Observability Stack

- Prometheus metrics collection
- Grafana dashboards
- Distributed tracing (Jaeger)
- Centralized logging (ELK Stack)

### Deployment Pipeline

- GitHub Actions CI/CD
- Docker image registry
- Kubernetes manifests
- Helm charts
- Environment-specific configurations

## 🤝 Katkıda Bulunma

1. Fork yapın
2. Feature branch oluşturun (`git checkout -b feature/amazing-feature`)
3. Changes'inizi commit edin (`git commit -m 'Add amazing feature'`)
4. Branch'inizi push edin (`git push origin feature/amazing-feature`)
5. Pull Request oluşturun

## 📞 Destek

- **Documentation:** `documentation/` klasöründeki detaylı kılavuzlar
- **API Examples:** Postman collection ve curl örnekleri
- **Troubleshooting:** Her bölümdeki sorun giderme rehberleri
- **GitHub Issues:** Sorun bildirimi ve öneriler için

## 📊 Proje Durumu

### ✅ Tamamlanan Bölümler (%70 Complete)

- **QuickStart Lab** - Tam hazır ve çalışır durumda
- **Order Service** - Clean Architecture + DDD + Outbox + CQRS tam implement
- **Comprehensive Documentation** - SAGA, Outbox, CQRS, Kafka Advanced, Event Sourcing
- **Inventory Service Domain Layer** - Entities, Value Objects, Domain Events

### 🔄 Devam Eden Bölümler

- **Inventory & Payment Services** - Application ve Infrastructure layers
- **SAGA Orchestrator** - Concrete implementation
- **Advanced Kafka** - Exactly-once semantics
- **Kubernetes & GKE** - Production deployment

📋 **Detaylı durum raporu:** [PROJECT-STATUS.md](./PROJECT-STATUS.md)

## 🎯 Final Hedef

Bu proje sonunda elde edeceğiniz skills:

- ✅ **Enterprise Microservices Architecture** tasarlama
- ✅ **Event-Driven Systems** geliştirme
- ✅ **Distributed Patterns** implementation
- ✅ **Production-Ready Applications** deployment
- ✅ **Cloud-Native Development** yaklaşımları
- ✅ **Advanced Kafka Programming** patterns
- ✅ **Kubernetes Operations** ve monitoring

## 🚀 Hızlı Başlangıç

Proje zaten solid bir foundation'a sahip. Hemen başlamak için:

```bash
# 1. Environment'ı başlat
cd microservices-quickstart
docker-compose up -d

# 2. Mevcut implementasyonu test et
./scripts/smoke-test.ps1

# 3. Durum raporunu incele
cat PROJECT-STATUS.md

# 4. Kaldığınız yerden devam edin
# Priority: Inventory Service Application Layer
```

Happy Coding! 🚀✨
