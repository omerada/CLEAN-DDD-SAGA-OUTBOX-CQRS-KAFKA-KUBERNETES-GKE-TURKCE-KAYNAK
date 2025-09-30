# QuickStart Mikroservisler Başlat Script - PowerShell Version
# Bu script tüm servisleri Docker Compose ile başlatır

Write-Host "🚀 Starting Microservices QuickStart Environment..." -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Yellow

# Dependency check
Write-Host "🔍 Checking dependencies..." -ForegroundColor Cyan

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "❌ Docker is not installed. Please install Docker first." -ForegroundColor Red
    exit 1
}

if (-not (Get-Command docker-compose -ErrorAction SilentlyContinue)) {
    Write-Host "❌ Docker Compose is not installed. Please install Docker Compose first." -ForegroundColor Red
    exit 1
}

Write-Host "✅ Docker and Docker Compose found!" -ForegroundColor Green

# Clean previous state
Write-Host "🧹 Cleaning previous containers and volumes..." -ForegroundColor Cyan
docker-compose down -v --remove-orphans

# Build and start services
Write-Host "🏗️ Building and starting services..." -ForegroundColor Cyan
docker-compose up --build -d

# Wait for services to be healthy
Write-Host "⏳ Waiting for services to be ready..." -ForegroundColor Cyan
Start-Sleep -Seconds 15

# Health check function
function Test-Port {
    param([string]$Host, [int]$Port)
    try {
        $connection = New-Object System.Net.Sockets.TcpClient($Host, $Port)
        $connection.Close()
        return $true
    }
    catch {
        return $false
    }
}

# Health check
Write-Host "🏥 Checking service health..." -ForegroundColor Cyan

$services = @(
    @{Name="PostgreSQL"; Port=5432},
    @{Name="Kafka"; Port=9092},
    @{Name="Order Service"; Port=8081},
    @{Name="Inventory Service"; Port=8082},
    @{Name="Payment Service"; Port=8083}
)

foreach ($service in $services) {
    if (Test-Port -Host "localhost" -Port $service.Port) {
        Write-Host "✅ $($service.Name) is healthy on port $($service.Port)" -ForegroundColor Green
    } else {
        Write-Host "❌ $($service.Name) is not responding on port $($service.Port)" -ForegroundColor Red
    }
}

# Display access information
Write-Host ""
Write-Host "🎉 QuickStart Environment is ready!" -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Yellow
Write-Host "📋 Service URLs:" -ForegroundColor Cyan
Write-Host "   Order Service:     http://localhost:8081/orders"
Write-Host "   Inventory Service: http://localhost:8082/inventory"
Write-Host "   Payment Service:   http://localhost:8083/payments"
Write-Host ""
Write-Host "🔧 Management URLs:" -ForegroundColor Cyan
Write-Host "   pgAdmin:          http://localhost:5050 (admin@admin.com / admin)"
Write-Host "   Order Health:     http://localhost:8081/actuator/health"
Write-Host "   Inventory Health: http://localhost:8082/actuator/health"
Write-Host "   Payment Health:   http://localhost:8083/actuator/health"
Write-Host ""
Write-Host "📊 Database Access:" -ForegroundColor Cyan
Write-Host "   PostgreSQL:       localhost:5432"
Write-Host "   Username:         admin"
Write-Host "   Password:         admin123"
Write-Host "   Databases:        order_db, inventory_db, payment_db"
Write-Host ""
Write-Host "📡 Kafka:" -ForegroundColor Cyan
Write-Host "   Broker:           localhost:9092"
Write-Host "   Topics:           orders, inventory, payments"
Write-Host ""
Write-Host "🧪 Next steps:" -ForegroundColor Cyan
Write-Host "   1. Run smoke tests:  .\scripts\smoke-test.ps1"
Write-Host "   2. Check logs:       docker-compose logs -f"
Write-Host "   3. Stop services:    .\scripts\stop.ps1"
Write-Host "================================================================" -ForegroundColor Yellow