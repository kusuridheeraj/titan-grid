# Titan Grid - Infrastructure Commands (Windows PowerShell)
# Note: For cross-platform compatibility, use the PowerShell scripts instead

.PHONY: help up down restart logs clean

help: ## Show this help message
	@powershell -Command "Write-Host 'Titan Grid - Infrastructure Commands' -ForegroundColor Cyan; Write-Host ''; Get-Content Makefile | Select-String -Pattern '^[a-zA-Z_-]+:.*?##' | ForEach-Object { $$_.Line -replace ':', '' -replace '##', '-' }"

up: ## Start all services
	docker-compose -f infra/docker-compose.yml up -d
	@powershell -Command "Write-Host '✓ All services started' -ForegroundColor Green; Write-Host 'Access:'; Write-Host '  - Aegis:   http://localhost:8080'; Write-Host '  - Cryptex: http://localhost:8081'; Write-Host '  - Nexus:   http://localhost:8082'; Write-Host '  - Grafana: http://localhost:3000'"

down: ## Stop all services
	docker-compose -f infra/docker-compose.yml down
	@powershell -Command "Write-Host '✓ All services stopped' -ForegroundColor Green"

restart: down up ## Restart all services

logs: ## Tail logs from all services
	docker-compose -f infra/docker-compose.yml logs -f

clean: ## Remove all containers, volumes, and build artifacts
	docker-compose -f infra/docker-compose.yml down -v
	@powershell -Command "if (Test-Path 'aegis/target') { Remove-Item -Recurse -Force aegis/target }; if (Test-Path 'cryptex/target') { Remove-Item -Recurse -Force cryptex/target }; if (Test-Path 'nexus/__pycache__') { Remove-Item -Recurse -Force nexus/__pycache__ }; Write-Host '✓ Cleaned all artifacts' -ForegroundColor Green"

aegis-test: ## Run Aegis tests
	cd aegis && mvn test

cryptex-test: ## Run Cryptex tests
	cd cryptex && mvn test

nexus-test: ## Run Nexus tests
	cd nexus && pytest

test-all: aegis-test cryptex-test nexus-test ## Run all tests

load-test: ## Run K6 load test against Aegis
	k6 run infra/tests/aegis-load-test.js
