# Titan Grid - PowerShell Helper Scripts
# Easier alternative to Makefile on Windows

function Show-Help {
    Write-Host "Titan Grid - Infrastructure Commands" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Available commands:" -ForegroundColor Yellow
    Write-Host "  .\scripts.ps1 up          - Start all services"
    Write-Host "  .\scripts.ps1 down        - Stop all services"
    Write-Host "  .\scripts.ps1 restart     - Restart all services"
    Write-Host "  .\scripts.ps1 logs        - Show logs from all services"
    Write-Host "  .\scripts.ps1 clean       - Remove all containers and build artifacts"
    Write-Host "  .\scripts.ps1 test-aegis  - Run Aegis tests"
    Write-Host "  .\scripts.ps1 test-cryptex - Run Cryptex tests"
    Write-Host "  .\scripts.ps1 test-nexus  - Run Nexus tests"
    Write-Host "  .\scripts.ps1 test-all    - Run all tests"
}

function Start-Services {
    Write-Host "Starting all services..." -ForegroundColor Yellow
    docker-compose -f infra/docker-compose.yml up -d
    Write-Host ""
    Write-Host "✓ All services started" -ForegroundColor Green
    Write-Host ""
    Write-Host "Access:" -ForegroundColor Cyan
    Write-Host "  - Aegis:   http://localhost:8080"
    Write-Host "  - Cryptex: http://localhost:8081"
    Write-Host "  - Nexus:   http://localhost:8082"
    Write-Host "  - Grafana: http://localhost:3000"
}

function Stop-Services {
    Write-Host "Stopping all services..." -ForegroundColor Yellow
    docker-compose -f infra/docker-compose.yml down
    Write-Host "✓ All services stopped" -ForegroundColor Green
}

function Restart-Services {
    Stop-Services
    Start-Services
}

function Show-Logs {
    Write-Host "Showing logs (Ctrl+C to exit)..." -ForegroundColor Yellow
    docker-compose -f infra/docker-compose.yml logs -f
}

function Clean-All {
    Write-Host "Cleaning all containers, volumes, and build artifacts..." -ForegroundColor Yellow
    docker-compose -f infra/docker-compose.yml down -v
    
    if (Test-Path "aegis/target") {
        Remove-Item -Recurse -Force "aegis/target"
        Write-Host "✓ Cleaned aegis/target" -ForegroundColor Gray
    }
    
    if (Test-Path "cryptex/target") {
        Remove-Item -Recurse -Force "cryptex/target"
        Write-Host "✓ Cleaned cryptex/target" -ForegroundColor Gray
    }
    
    if (Test-Path "nexus/__pycache__") {
        Remove-Item -Recurse -Force "nexus/__pycache__"
        Write-Host "✓ Cleaned nexus/__pycache__" -ForegroundColor Gray
    }
    
    Write-Host ""
    Write-Host "✓ All artifacts cleaned" -ForegroundColor Green
}

function Test-Aegis {
    Write-Host "Running Aegis tests..." -ForegroundColor Yellow
    Set-Location aegis
    mvn test
    Set-Location ..
}

function Test-Cryptex {
    Write-Host "Running Cryptex tests..." -ForegroundColor Yellow
    Set-Location cryptex
    mvn test
    Set-Location ..
}

function Test-Nexus {
    Write-Host "Running Nexus tests..." -ForegroundColor Yellow
    Set-Location nexus
    pytest
    Set-Location ..
}

function Test-All {
    Test-Aegis
    Test-Cryptex
    Test-Nexus
}

# Main script logic
param(
    [Parameter(Position=0)]
    [string]$Command = "help"
)

switch ($Command.ToLower()) {
    "up" { Start-Services }
    "down" { Stop-Services }
    "restart" { Restart-Services }
    "logs" { Show-Logs }
    "clean" { Clean-All }
    "test-aegis" { Test-Aegis }
    "test-cryptex" { Test-Cryptex }
    "test-nexus" { Test-Nexus }
    "test-all" { Test-All }
    "help" { Show-Help }
    default {
        Write-Host "Unknown command: $Command" -ForegroundColor Red
        Write-Host ""
        Show-Help
    }
}
