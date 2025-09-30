# QuickStart Durdur Script - PowerShell Version

Write-Host "🛑 Stopping Microservices QuickStart Environment..." -ForegroundColor Yellow
Write-Host "=================================================" -ForegroundColor Yellow

# Stop and remove containers, networks, volumes
Write-Host "🔄 Stopping services..." -ForegroundColor Cyan
docker-compose down

Write-Host "🧹 Cleaning up volumes and networks..." -ForegroundColor Cyan
docker-compose down -v --remove-orphans

# Optional: Remove images (uncomment if you want to clean everything)
# Write-Host "🗑️ Removing built images..." -ForegroundColor Cyan
# docker rmi $(docker images 'microservices-quickstart*' -q) 2>$null

Write-Host "✅ Environment stopped successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "💡 To start again, run: .\scripts\start.ps1" -ForegroundColor Cyan