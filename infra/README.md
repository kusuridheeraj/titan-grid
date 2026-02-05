# 🏗️ Titan Grid Infrastructure

This directory contains all infrastructure-as-code for the Titan Grid platform.

## 📦 Services

### Core Services
- **Redis** (Port 6379) - Rate limiting & caching
- **PostgreSQL** (Port 5432) - Persistent data storage
- **HashiCorp Vault** (Port 8200) - Secrets management

### Observability Stack
- **Prometheus** (Port 9090) - Metrics collection
- **Grafana** (Port 3000) - Metrics visualization

---

## 🚀 Quick Start

### Start All Services
```powershell
# From project root
cd c:\PlayStation\assets\titan-grid

# Start services
docker-compose -f infra/docker-compose.yml up -d

# Or use the helper script
.\scripts.ps1 up
```

### Verify Services
```powershell
# Check running containers
docker ps

# Check service health
docker-compose -f infra/docker-compose.yml ps
```

### Access Services
- **Redis:** `redis-cli -h localhost -p 6379 -a titan_redis_secure_2026`
- **PostgreSQL:** `psql -h localhost -p 5432 -U titan_admin -d titan_grid`
- **Vault:** http://localhost:8200 (Token: `dev-root-token-titan-2026`)
- **Prometheus:** http://localhost:9090
- **Grafana:** http://localhost:3000 (admin/admin)

---

## 🔧 Service Details

### Redis
**Purpose:** High-performance in-memory store for rate limiting

**Test Connection:**
```powershell
# Using redis-cli
docker exec -it titan-redis redis-cli -a titan_redis_secure_2026 ping
# Expected: PONG

# Set and get a test key
docker exec -it titan-redis redis-cli -a titan_redis_secure_2026 SET test "Hello Titan"
docker exec -it titan-redis redis-cli -a titan_redis_secure_2026 GET test
```

### PostgreSQL
**Purpose:** Relational database for persistent storage

**Test Connection:**
```powershell
# Connect to database
docker exec -it titan-postgres psql -U titan_admin -d titan_grid

# List schemas
\dn

# List tables in aegis schema
\dt aegis.*

# Exit
\q
```

**Schemas:**
- `aegis` - Rate limiting events and blacklist
- `cryptex` - File metadata
- `nexus` - AI agent activity logs

### HashiCorp Vault
**Purpose:** Centralized secrets management

**Test Connection:**
```powershell
# Check Vault status
docker exec -it titan-vault vault status

# Login
docker exec -it titan-vault vault login dev-root-token-titan-2026

# Enable Transit engine (for encryption)
docker exec -it titan-vault vault secrets enable transit
```

---

## 📊 Observability

### Prometheus
Access metrics at http://localhost:9090

**Sample Queries:**
```promql
# JVM memory usage (Aegis)
jvm_memory_used_bytes{job="aegis"}

# HTTP request rate
rate(http_requests_total[1m])
```

### Grafana
Access dashboards at http://localhost:3000

**Initial Setup:**
1. Login: admin/admin
2. Add Prometheus as data source: http://prometheus:9090
3. Import dashboards from `grafana/dashboards/`

---

## 🛑 Stop Services

```powershell
# Stop all services
docker-compose -f infra/docker-compose.yml down

# Stop and remove volumes (⚠️ deletes all data)
docker-compose -f infra/docker-compose.yml down -v
```

---

## 🔐 Security Notes

### Development vs Production

**Current Setup (Development):**
- ✅ Simple passwords in `.env`
- ✅ Vault in dev mode (no TLS)
- ✅ Services exposed on localhost

**Production Setup (Future):**
- 🔒 Secrets in Azure Key Vault / AWS Secrets Manager
- 🔒 Vault with TLS and proper unsealing
- 🔒 Services behind API Gateway
- 🔒 Network isolation with VPCs

---

## 🧪 Health Checks

All services have health checks configured:

```powershell
# View health status
docker-compose -f infra/docker-compose.yml ps

# Healthy services show: Up (healthy)
```

---

## 📝 Logs

```powershell
# View all logs
docker-compose -f infra/docker-compose.yml logs -f

# View specific service
docker-compose -f infra/docker-compose.yml logs -f redis
docker-compose -f infra/docker-compose.yml logs -f postgres
docker-compose -f infra/docker-compose.yml logs -f vault
```

---

## 🔄 Troubleshooting

### Port Already in Use
```powershell
# Find process using port
netstat -ano | findstr :6379

# Kill process (replace PID)
taskkill /PID <PID> /F
```

### Container Won't Start
```powershell
# Check logs
docker logs titan-redis
docker logs titan-postgres
docker logs titan-vault

# Restart specific service
docker-compose -f infra/docker-compose.yml restart redis
```

### Reset Everything
```powershell
# Nuclear option: delete everything and start fresh
docker-compose -f infra/docker-compose.yml down -v
docker-compose -f infra/docker-compose.yml up -d
```

---

## ✅ Day 1 Checklist

- [ ] All containers running (`docker ps`)
- [ ] Redis responds to PING
- [ ] PostgreSQL schemas created
- [ ] Vault is accessible
- [ ] Prometheus collecting metrics
- [ ] Grafana accessible

**Next:** Day 2 - Start building Aegis (Rate Limiter)
