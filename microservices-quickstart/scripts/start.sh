#!/bin/bash

# QuickStart Mikroservisler Başlat Script
# Bu script tüm servisleri Docker Compose ile başlatır

echo "🚀 Starting Microservices QuickStart Environment..."
echo "================================================================"

# Dependency check
echo "🔍 Checking dependencies..."

if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

echo "✅ Docker and Docker Compose found!"

# Clean previous state
echo "🧹 Cleaning previous containers and volumes..."
docker-compose down -v --remove-orphans

# Build and start services
echo "🏗️ Building and starting services..."
docker-compose up --build -d

# Wait for services to be healthy
echo "⏳ Waiting for services to be ready..."
sleep 10

# Health check
echo "🏥 Checking service health..."

services=("postgres:5432" "kafka:9092" "order-service:8081" "inventory-service:8082" "payment-service:8083")
for service in "${services[@]}"; do
    IFS=':' read -r name port <<< "$service"
    if nc -z localhost $port 2>/dev/null; then
        echo "✅ $name is healthy on port $port"
    else
        echo "❌ $name is not responding on port $port"
    fi
done

# Display access information
echo ""
echo "🎉 QuickStart Environment is ready!"
echo "================================================================"
echo "📋 Service URLs:"
echo "   Order Service:     http://localhost:8081/orders"
echo "   Inventory Service: http://localhost:8082/inventory"
echo "   Payment Service:   http://localhost:8083/payments"
echo ""
echo "🔧 Management URLs:"
echo "   pgAdmin:          http://localhost:5050 (admin@admin.com / admin)"
echo "   Order Health:     http://localhost:8081/actuator/health"
echo "   Inventory Health: http://localhost:8082/actuator/health"
echo "   Payment Health:   http://localhost:8083/actuator/health"
echo ""
echo "📊 Database Access:"
echo "   PostgreSQL:       localhost:5432"
echo "   Username:         admin"
echo "   Password:         admin123"
echo "   Databases:        order_db, inventory_db, payment_db"
echo ""
echo "📡 Kafka:"
echo "   Broker:           localhost:9092"
echo "   Topics:           orders, inventory, payments"
echo ""
echo "🧪 Next steps:"
echo "   1. Run smoke tests:  ./scripts/smoke-test.sh"
echo "   2. Check logs:       docker-compose logs -f"
echo "   3. Stop services:    ./scripts/stop.sh"
echo "================================================================"