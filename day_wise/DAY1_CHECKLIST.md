# ✅ Day 1 Completion Checklist

## 📦 What We've Created

### Infrastructure Files
- ✅ [docker-compose.yml](file:///c:/PlayStation/assets/titan-grid/infra/docker-compose.yml) - 5 services configured
- ✅ [01-init-database.sql](file:///c:/PlayStation/assets/titan-grid/infra/init-scripts/01-init-database.sql) - Database schemas
- ✅ [prometheus.yml](file:///c:/PlayStation/assets/titan-grid/infra/prometheus/prometheus.yml) - Metrics configuration
- ✅ [.env](file:///c:/PlayStation/assets/titan-grid/.env) - Environment variables
- ✅ [infra/README.md](file:///c:/PlayStation/assets/titan-grid/infra/README.md) - Complete documentation

### Services Configured
1. **Redis** (Port 6379) - Rate limiting & caching
2. **PostgreSQL** (Port 5432) - Database with 3 schemas (aegis, cryptex, nexus)
3. **HashiCorp Vault** (Port 8200) - Secrets management
4. **Prometheus** (Port 9090) - Metrics collection
5. **Grafana** (Port 3000) - Metrics visualization

---

## 🚀 Next Steps: Start Docker and Verify

### Step 1: Start Docker Desktop

**Option A: Start Menu**
1. Press `Win` key
2. Type "Docker Desktop"
3. Click to launch
4. Wait for "Docker Desktop is running" in system tray

**Option B: Command Line**
```powershell
# This should open Docker Desktop
Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"

# Wait ~30 seconds, then check status
docker ps
```

---

### Step 2: Launch All Services

Once Docker Desktop is running:

```powershell
# Navigate to project root
cd c:\PlayStation\assets\titan-grid

# Start all services (this will download images first time)
docker-compose -f infra/docker-compose.yml up -d

# Or use our helper script
.\scripts.ps1 up
```

**Expected Output:**
```
Creating network "infra_titan-network" ... done
Creating volume "infra_redis-data" ... done
Creating volume "infra_postgres-data" ... done
Creating volume "infra_prometheus-data" ... done
Creating volume "infra_grafana-data" ... done
Pulling redis (redis:7-alpine)...
Pulling postgres (postgres:15-alpine)...
Pulling vault (hashicorp/vault:latest)...
Pulling prometheus (prom/prometheus:latest)...
Pulling grafana (grafana/grafana:latest)...
Creating titan-redis ... done
Creating titan-postgres ... done
Creating titan-vault ... done
Creating titan-prometheus ... done
Creating titan-grafana ... done
```

---

### Step 3: Verify All Services

```powershell
# Check running containers
docker ps

# Expected: 5 containers running (redis, postgres, vault, prometheus, grafana)
```

---

### Step 4: Test Connectivity

#### Test Redis
```powershell
docker exec -it titan-redis redis-cli -a titan_redis_secure_2026 ping
# Expected: PONG
```

#### Test PostgreSQL
```powershell
docker exec -it titan-postgres psql -U titan_admin -d titan_grid -c "\dn"
# Expected: Shows aegis, cryptex, nexus schemas
```

#### Test Vault
```powershell
docker exec -it titan-vault vault status
# Expected: Shows Vault status (unsealed in dev mode)
```

#### Test Web UIs
- Vault: http://localhost:8200 (Token: `dev-root-token-titan-2026`)
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (Login: admin/admin)

---

## ✅ Completion Criteria

Day 1 is complete when:
- [x] Docker Desktop is running
- [x] All 5 containers are running (`docker ps` shows 5)
- [x] Redis responds to PING ✅ (Verified: PONG)
- [x] PostgreSQL has 3 schemas (aegis, cryptex, nexus) ✅ (Verified)
- [x] Vault status is accessible ✅ (Unsealed, Version 1.21.3)
- [x] Prometheus UI loads at :9090 ✅
- [x] Grafana UI loads at :3000 ✅

---

## 🎉 Day 1 Complete!

**Day 2-3: Aegis Core (Rate Limiter)**
- Initialize Spring Boot project
- Write Lua script for Sliding Window algorithm
- Implement RateLimiterService with Redis

**When ready, message me:**
> "Day 1 complete! Ready for Day 2"

---

## 🛑 If You Get Errors

### Port Already in Use
```powershell
# Find what's using the port (e.g., 6379 for Redis)
netstat -ano | findstr :6379

# Kill the process
taskkill /PID <PID> /F
```

### Container Won't Start
```powershell
# View logs
docker logs titan-redis
docker logs titan-postgres

# Restart specific container
docker restart titan-redis
```

### Complete Reset
```powershell
# Stop everything and delete volumes
docker-compose -f infra/docker-compose.yml down -v

# Start fresh
docker-compose -f infra/docker-compose.yml up -d
```

---

## 📖 Documentation

For detailed commands and troubleshooting, see:
- [infra/README.md](file:///c:/PlayStation/assets/titan-grid/infra/README.md)

---

**Current Status:** Infrastructure configured ✅  
**Action Required:** Start Docker Desktop and run `docker-compose up -d`
