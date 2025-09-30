# QuickStart Smoke Test - PowerShell Version
# Bu script temel API endpoint'lerini test eder

Write-Host "🧪 Running QuickStart Smoke Tests..." -ForegroundColor Green
Write-Host "====================================" -ForegroundColor Yellow

$testResults = @()
$baseDelay = 2

# Test function
function Invoke-ApiTest {
    param(
        [string]$Name,
        [string]$Url,
        [string]$Method = "GET",
        [hashtable]$Body = $null,
        [int]$ExpectedStatus = 200
    )
    
    Write-Host "🔍 Testing: $Name" -ForegroundColor Cyan
    
    try {
        $headers = @{
            'Content-Type' = 'application/json'
        }
        
        if ($Body) {
            $jsonBody = $Body | ConvertTo-Json
            $response = Invoke-RestMethod -Uri $Url -Method $Method -Body $jsonBody -Headers $headers
        } else {
            $response = Invoke-RestMethod -Uri $Url -Method $Method -Headers $headers
        }
        
        Write-Host "✅ $Name - SUCCESS" -ForegroundColor Green
        return @{Name=$Name; Status="PASS"; Response=$response}
    }
    catch {
        Write-Host "❌ $Name - FAILED: $($_.Exception.Message)" -ForegroundColor Red
        return @{Name=$Name; Status="FAIL"; Error=$_.Exception.Message}
    }
}

# Wait for services to be ready
Write-Host "⏳ Waiting for services to warm up..." -ForegroundColor Cyan
Start-Sleep -Seconds $baseDelay

# Health Check Tests
Write-Host "`n🏥 Health Check Tests" -ForegroundColor Yellow
$testResults += Invoke-ApiTest -Name "Order Service Health" -Url "http://localhost:8081/actuator/health"
$testResults += Invoke-ApiTest -Name "Inventory Service Health" -Url "http://localhost:8082/actuator/health"  
$testResults += Invoke-ApiTest -Name "Payment Service Health" -Url "http://localhost:8083/actuator/health"

Start-Sleep -Seconds $baseDelay

# Basic CRUD Tests
Write-Host "`n📋 Basic CRUD Tests" -ForegroundColor Yellow

# Test Order Service
$orderPayload = @{
    customerId = "CUST-TEST-001"
    productId = "PROD-TEST-123"
    quantity = 2
    price = 99.99
}
$orderResult = Invoke-ApiTest -Name "Create Order" -Url "http://localhost:8081/orders" -Method "POST" -Body $orderPayload
$testResults += $orderResult

if ($orderResult.Status -eq "PASS") {
    $testResults += Invoke-ApiTest -Name "List Orders" -Url "http://localhost:8081/orders"
    $testResults += Invoke-ApiTest -Name "Get Order by Customer" -Url "http://localhost:8081/orders/customer/CUST-TEST-001"
}

Start-Sleep -Seconds $baseDelay

# Test Inventory Service
$testResults += Invoke-ApiTest -Name "Check Inventory" -Url "http://localhost:8082/inventory/PROD-TEST-123"
$testResults += Invoke-ApiTest -Name "List All Inventory" -Url "http://localhost:8082/inventory"

$reservePayload = @{
    productId = "PROD-TEST-123"
    quantity = 1
    orderId = "1"
}
$testResults += Invoke-ApiTest -Name "Reserve Inventory" -Url "http://localhost:8082/inventory/reserve" -Method "POST" -Body $reservePayload

Start-Sleep -Seconds $baseDelay

# Test Payment Service
$paymentPayload = @{
    orderId = "1"
    amount = 199.98
    paymentMethod = "CREDIT_CARD"
    cardToken = "tok_test_123456"
}
$paymentResult = Invoke-ApiTest -Name "Process Payment" -Url "http://localhost:8083/payments" -Method "POST" -Body $paymentPayload
$testResults += $paymentResult

if ($paymentResult.Status -eq "PASS") {
    $testResults += Invoke-ApiTest -Name "Get Payment by Order" -Url "http://localhost:8083/payments/order/1"
}

$testResults += Invoke-ApiTest -Name "List All Payments" -Url "http://localhost:8083/payments"

# Results Summary
Write-Host "`n📊 Test Results Summary" -ForegroundColor Yellow
Write-Host "========================" -ForegroundColor Yellow

$passCount = ($testResults | Where-Object { $_.Status -eq "PASS" }).Count
$failCount = ($testResults | Where-Object { $_.Status -eq "FAIL" }).Count
$totalCount = $testResults.Count

foreach ($result in $testResults) {
    if ($result.Status -eq "PASS") {
        Write-Host "✅ $($result.Name)" -ForegroundColor Green
    } else {
        Write-Host "❌ $($result.Name) - $($result.Error)" -ForegroundColor Red
    }
}

Write-Host "`n📈 Overall Results:" -ForegroundColor Cyan
Write-Host "   Total Tests: $totalCount"
Write-Host "   Passed: $passCount" -ForegroundColor Green
Write-Host "   Failed: $failCount" -ForegroundColor Red
Write-Host "   Success Rate: $([math]::Round(($passCount / $totalCount) * 100, 2))%"

if ($failCount -eq 0) {
    Write-Host "`n🎉 All tests passed! Your QuickStart environment is working correctly." -ForegroundColor Green
} else {
    Write-Host "`n⚠️ Some tests failed. Check the service logs with: docker-compose logs" -ForegroundColor Yellow
}

Write-Host "`n🔗 Quick Access URLs:" -ForegroundColor Cyan
Write-Host "   Order Service:     http://localhost:8081/orders"
Write-Host "   Inventory Service: http://localhost:8082/inventory"
Write-Host "   Payment Service:   http://localhost:8083/payments"
Write-Host "   pgAdmin:          http://localhost:5050"