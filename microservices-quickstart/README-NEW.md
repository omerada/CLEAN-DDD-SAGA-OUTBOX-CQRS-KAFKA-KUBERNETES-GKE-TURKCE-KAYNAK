# 🚀 Mikroservisler QuickStart Guide

[![Java](https://img.shields.io/badge/Java-17+-brightgreen.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-20+-blue.svg)](https://www.docker.com/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-7.4-orange.svg)](https://kafka.apache.org/)

Bu QuickStart Guide, **Spring Boot**, **Kafka** ve **PostgreSQL** kullanarak mikroservis mimarisini öğrenmek için tasarlanmış 2-3 saatlik yoğun bir laboratuvar deneyimidir.

## 🎯 Öğreneceğiniz Konular

- ✅ **Mikroservis Mimarisi** - Service-per-database pattern
- ✅ **Event-Driven Architecture** - Kafka ile asynchronous messaging
- ✅ **REST API Design** - RESTful endpoint'ler ve HTTP best practices
- ✅ **Docker Compose** - Multi-container orchestration
- ✅ **Database Per Service** - PostgreSQL ile veri izolasyonu
- ✅ **Health Monitoring** - Spring Actuator ile observability

## 🏗️ Proje Yapısı

```
microservices-quickstart/
├── 📁 documentation/                 # Öğrenme materyalleri
│   └── 01-quickstart/
├── 📁 order-service/                 # Sipariş yönetimi
│   ├── Dockerfile
│   ├── pom.xml
│   └── 📁 src/main/java/com/example/order/
├── 📁 inventory-service/             # Stok yönetimi
│   ├── Dockerfile
│   ├── pom.xml
│   └── 📁 src/main/java/com/example/inventory/
├── 📁 payment-service/               # Ödeme işlemleri
│   ├── Dockerfile
│   ├── pom.xml
│   └── 📁 src/main/java/com/example/payment/
├── 📁 scripts/                       # Automation scripts
│   ├── start.ps1                     # Sistem başlat (Windows)
│   ├── start.sh                      # Sistem başlat (Linux/Mac)
│   ├── smoke-test.ps1                # Test suite (Windows)
│   └── stop.ps1                      # Sistem durdur
├── docker-compose.yml                # Container orchestration
└── README.md                         # Bu dosya
```

## ⚡ Hızlı Başlangıç

### 1. Prerequisites Kontrolü

```powershell
# Java kontrolü
java -version
# Beklenen: openjdk version "17" veya üzeri

# Maven kontrolü
mvn -version
# Beklenen: Apache Maven 3.8+

# Docker kontrolü
docker --version
docker-compose --version
# Beklenen: Docker 20+, Docker Compose 2.0+
```

### 2. Sistem Başlatma

```powershell
# Windows PowerShell
cd microservices-quickstart
.\scripts\start.ps1

# Linux/MacOS Bash
cd microservices-quickstart
chmod +x scripts/start.sh
./scripts/start.sh
```

### 3. Sistem Testi

```powershell
# Smoke test çalıştırma
.\scripts\smoke-test.ps1

# Manuel test
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

## 🔗 Servis Endpoint'leri

| Servis                | URL                             | Açıklama               |
| --------------------- | ------------------------------- | ---------------------- |
| **Order Service**     | http://localhost:8081/orders    | Sipariş CRUD işlemleri |
| **Inventory Service** | http://localhost:8082/inventory | Stok yönetimi          |
| **Payment Service**   | http://localhost:8083/payments  | Ödeme işlemleri        |

### Health Check Endpoint'leri

- Order Health: http://localhost:8081/actuator/health
- Inventory Health: http://localhost:8082/actuator/health
- Payment Health: http://localhost:8083/actuator/health

## 💾 Database Erişimi

| Kaynak         | Bilgi     | Değer                              |
| -------------- | --------- | ---------------------------------- |
| **PostgreSQL** | Host:Port | localhost:5432                     |
|                | Username  | admin                              |
|                | Password  | admin123                           |
|                | Databases | order_db, inventory_db, payment_db |
| **pgAdmin**    | URL       | http://localhost:5050              |
|                | Login     | admin@admin.com / admin            |

## 📡 Kafka Bilgileri

- **Broker**: localhost:9092
- **Auto-created Topics**: orders, inventory, payments
- **Consumer Groups**: order-service-group, inventory-service-group, payment-service-group

## 🧪 API Test Örnekleri

### Sipariş Oluşturma

```powershell
$order = @{
    customerId = "CUST-001"
    productId = "PROD-123"
    quantity = 2
    price = 99.99
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/orders" -Method POST -Body $order -ContentType "application/json"
```

### Stok Kontrolü

```powershell
Invoke-RestMethod -Uri "http://localhost:8082/inventory/PROD-123" -Method GET
```

### Ödeme İşlemi

```powershell
$payment = @{
    orderId = "1"
    amount = 199.98
    paymentMethod = "CREDIT_CARD"
    cardToken = "tok_123456"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8083/payments" -Method POST -Body $payment -ContentType "application/json"
```

## 🛠️ Troubleshooting

### Yaygın Problemler

#### Port zaten kullanımda

```powershell
# Portları kontrol et
netstat -an | findstr :5432
netstat -an | findstr :9092

# Docker containers'ı temizle
docker-compose down -v --remove-orphans
```

#### Maven build hatası

```powershell
# Maven cache temizle
mvn clean compile -U
mvn dependency:purge-local-repository
```

#### Docker build fails

```powershell
# Docker cache temizle
docker builder prune -f
docker system prune -f
```

## 📊 İzleme ve Loglar

```powershell
# Tüm servis logları
docker-compose logs -f

# Spesifik servis logları
docker-compose logs -f order-service
docker-compose logs -f kafka

# Container durumları
docker-compose ps
```

## 🛑 Sistem Durdurma

```powershell
# Servisleri durdur
.\scripts\stop.ps1

# Veya manuel
docker-compose down -v --remove-orphans
```

## ⏭️ Sıradaki Adımlar

1. **📚 Clean Architecture** - [Bölüm 2](documentation/02-clean-architecture/README.md)
2. **🏛️ Hexagonal Architecture** - Ports & Adapters pattern
3. **🎯 Domain Driven Design** - Strategic ve tactical patterns
4. **🔄 SAGA Pattern** - Distributed transaction management

## 🤝 Katkı

Bu proje açık kaynak ruhunda geliştirilmektedir. Katkılarınızı memnuniyetle karşılıyoruz:

- 🐛 **Bug Report**: Issues bölümünden hata bildirin
- 💡 **Feature Request**: Yeni özellik önerileri
- 📖 **Documentation**: Dökümantasyon iyileştirmeleri
- 🔧 **Code Contribution**: Pull request gönderin

---

**🎯 Hedef**: Bu QuickStart ile mikroservis ekosisteminin temellerini öğrenmek ve sonraki aşamalara hazır olmak.

**⏱️ Süre**: 2-3 saat

**🚀 Başarı Kriterleri**: Tüm servisler ayakta, API'ler çalışıyor, basic event flow test edildi.

## 📚 Detaylı Dökümantasyon

Kapsamlı öğrenme materyalleri için [QuickStart Lab dökümantasyonuna](documentation/01-quickstart/README.md) bakın.
