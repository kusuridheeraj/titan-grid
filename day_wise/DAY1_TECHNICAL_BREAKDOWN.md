# 🔍 Day 1 Technical Breakdown: The `.env` File Issue

**Date:** 2026-02-05  
**Issue:** PostgreSQL container crashing, environment variables not loading  
**Status:** ✅ RESOLVED

---

## 📊 The Problem

### What You Saw
When you first ran `docker-compose up -d`, you got these warnings:
```
time="2026-02-05T18:19:24+05:30" level=warning msg="The \"REDIS_PASSWORD\" variable is not set. Defaulting to a blank string."
time="2026-02-05T18:19:24+05:30" level=warning msg="The \"POSTGRES_USER\" variable is not set. Defaulting to a blank string."
time="2026-02-05T18:19:24+05:30" level=warning msg="The \"POSTGRES_PASSWORD\" variable is not set. Defaulting to a blank string."
```

Then when you checked containers:
```powershell
docker ps
# Showed: titan-postgres was "Restarting (1) 29 seconds ago"
```

PostgreSQL was **crash-looping** (starting, crashing, restarting repeatedly).

---

## 🧠 Root Cause Analysis

### Why Did This Happen?

**The File Structure Problem:**
```
titan-grid/
├── .env                              ← Environment variables HERE (root)
└── infra/
    └── docker-compose.yml            ← Docker Compose file HERE (infra/)
```

### How Docker Compose Loads `.env` Files

Docker Compose has a specific search pattern for `.env` files:

1. **First, it looks in the SAME directory as `docker-compose.yml`**
2. **NOT in the parent directory**

So when you ran:
```powershell
docker-compose -f infra/docker-compose.yml up -d
```

Docker Compose looked for `.env` in:
- ❌ `c:\PlayStation\assets\titan-grid\infra\.env` (doesn't exist)

It did NOT look in:
- ✅ `c:\PlayStation\assets\titan-grid\.env` (exists but ignored)

---

## 🔧 What Happened in `docker-compose.yml`

### The Configuration
```yaml
postgres:
  image: postgres:15-alpine
  environment:
    POSTGRES_USER: ${POSTGRES_USER}        # ← Expected from .env
    POSTGRES_PASSWORD: ${POSTGRES_PASSWORD} # ← Expected from .env
    POSTGRES_DB: ${POSTGRES_DB}            # ← Expected from .env
```

### What Docker Saw (Before Fix)
Since `.env` wasn't loaded, Docker replaced variables with **empty strings**:
```yaml
postgres:
  environment:
    POSTGRES_USER: ""           # ❌ BLANK!
    POSTGRES_PASSWORD: ""       # ❌ BLANK!
    POSTGRES_DB: ""            # ❌ BLANK!
```

### Why PostgreSQL Crashed
PostgreSQL requires:
- A valid `POSTGRES_USER` (can't be blank)
- A valid `POSTGRES_PASSWORD` (can't be blank)
- A valid `POSTGRES_DB` (can't be blank)

When it received empty strings, it **failed validation** and exited with error code 1.

Docker's **restart policy** (`restart: unless-stopped`) kept trying to restart it, causing the crash loop.

---

## ✅ The Solution

### Commands I Used

#### Step 1: Diagnosed the Issue
```powershell
# Checked container status
docker ps
# Output: titan-postgres was "Restarting (1) 29 seconds ago"

# Checked PostgreSQL logs
docker logs titan-postgres --tail 20
# Output: Error about missing POSTGRES_USER

# Verified .env file exists in root
Test-Path .env
# Output: True

# Checked .env content
Get-Content .env | Select-Object -First 5
# Output: Shows REDIS_PASSWORD, POSTGRES_USER, etc.
```

#### Step 2: Fixed the `.env` Location
```powershell
# Copy .env from root to infra/ directory
Copy-Item .env infra\.env
```

**What This Did:**
- Created: `c:\PlayStation\assets\titan-grid\infra\.env`
- Now Docker Compose can find it when running from `infra/docker-compose.yml`

#### Step 3: Restarted Containers
```powershell
# Stop all containers and remove them
docker-compose -f infra/docker-compose.yml down

# Start fresh with new .env loaded
docker-compose -f infra/docker-compose.yml up -d
```

**What This Did:**
- `down`: Stopped and removed all containers (but kept volumes with data)
- `up -d`: Started containers in detached mode, now reading `infra/.env`

---

## 📈 Before vs After

### BEFORE Fix

**File Structure:**
```
titan-grid/
├── .env                    ← Variables defined HERE
└── infra/
    └── docker-compose.yml  ← Looking for .env HERE ❌
```

**What Docker Saw:**
```yaml
POSTGRES_USER: ""
POSTGRES_PASSWORD: ""
POSTGRES_DB: ""
```

**Result:**
- PostgreSQL: ❌ Crash loop
- Redis: ✅ Running (doesn't require password)
- Vault: ✅ Running (dev mode, no validation)
- Prometheus: ✅ Running (no env vars needed)
- Grafana: ✅ Running (hardcoded credentials)

---

### AFTER Fix

**File Structure:**
```
titan-grid/
├── .env                    ← Still here (backup)
└── infra/
    ├── .env                ← NOW ALSO HERE ✅
    └── docker-compose.yml  ← Reads this .env ✅
```

**What Docker Saw:**
```yaml
POSTGRES_USER: "titan_admin"
POSTGRES_PASSWORD: "titan_postgres_secure_2026"
POSTGRES_DB: "titan_grid"
```

**Result:**
- PostgreSQL: ✅ Running, healthy, schemas created
- Redis: ✅ Running with password protection
- Vault: ✅ Running with proper token
- Prometheus: ✅ Running
- Grafana: ✅ Running

---

## 🧪 Verification Commands

### What I Ran to Confirm Fix

```powershell
# 1. Check all containers running
docker ps
# Expected: 5 containers, all "Up X minutes"

# 2. Test Redis connection
docker exec -it titan-redis redis-cli -a titan_redis_secure_2026 ping
# Expected: PONG
# Result: ✅ PONG

# 3. Test PostgreSQL connection and check schemas
docker exec -it titan-postgres psql -U titan_admin -d titan_grid -c "\dn"
# Expected: Shows aegis, cryptex, nexus schemas
# Result: ✅ All 3 schemas present

# 4. Test Vault status
docker exec titan-vault sh -c 'VAULT_ADDR=http://127.0.0.1:8200 vault status'
# Expected: Unsealed, Initialized, Version info
# Result: ✅ Unsealed, Version 1.21.3
```

---

## 🎓 Key Learning Points

### 1. Docker Compose `.env` File Location Rules

**Rule:** `.env` must be in the **same directory** as `docker-compose.yml`

**Common Patterns:**

**Pattern A: Root-level docker-compose.yml**
```
project/
├── .env
├── docker-compose.yml  ✅ Reads .env from same level
```

**Pattern B: Nested docker-compose.yml (What we have)**
```
project/
├── .env               ❌ Ignored!
└── infra/
    ├── .env           ✅ Reads this one
    └── docker-compose.yml
```

**Pattern C: Explicit --env-file flag**
```powershell
docker-compose -f infra/docker-compose.yml --env-file .env up -d
```
This works but requires remembering the flag every time.

---

### 2. Variable Substitution in Docker Compose

**In `docker-compose.yml`:**
```yaml
environment:
  MY_VAR: ${MY_VAR}   # Reads from .env file
  MY_VAR: "hardcoded" # No substitution, literal value
```

**If variable not found:**
- Default behavior: Use empty string `""`
- You can set defaults: `${MY_VAR:-default_value}`

---

### 3. Container Restart Policies

**In our `docker-compose.yml`:**
```yaml
restart: unless-stopped
```

**What this means:**
- Container crashes? → Docker automatically restarts it
- You manually stop it? → Docker doesn't restart it
- System reboots? → Docker restarts it on boot

**Why PostgreSQL was "always restarting":**
- Crash (empty credentials) → Docker restarts it
- Crashes again → Docker restarts it
- **Loop continues forever** until we fix the root cause

---

### 4. Health Checks

**In `docker-compose.yml`:**
```yaml
postgres:
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER}"]
    interval: 10s
    timeout: 5s
    retries: 5
```

**What this does:**
- Every 10 seconds, Docker runs `pg_isready -U titan_admin`
- If it succeeds 5 times in a row → Status: "healthy"
- If it fails → Status: "unhealthy"
- Container crashes → Status: "starting" or "restarting"

**How to check:**
```powershell
docker ps  # Shows (healthy) or (unhealthy) next to status
```

---

## 🔍 Debugging Workflow

### If you ever face container issues again:

**Step 1: Check Status**
```powershell
docker ps -a  # -a shows ALL containers, even stopped ones
```

**Step 2: Check Logs**
```powershell
docker logs <container-name> --tail 50
docker logs <container-name> --follow  # Live tail
```

**Step 3: Check Environment Variables**
```powershell
# See what variables the container received
docker exec <container-name> env
```

**Step 4: Interactive Shell (for debugging)**
```powershell
# Open a shell inside the running container
docker exec -it <container-name> sh

# For postgres specifically:
docker exec -it titan-postgres psql -U titan_admin -d titan_grid
```

**Step 5: Restart Individual Container**
```powershell
docker restart <container-name>
```

**Step 6: Nuclear Option (fresh start)**
```powershell
docker-compose -f infra/docker-compose.yml down -v  # -v removes volumes too!
docker-compose -f infra/docker-compose.yml up -d
```

---

## 📁 Files Created/Modified

### What I Created:
1. ✅ `infra/docker-compose.yml` - Main infrastructure configuration
2. ✅ `infra/init-scripts/01-init-database.sql` - PostgreSQL schema initialization
3. ✅ `infra/prometheus/prometheus.yml` - Metrics configuration
4. ✅ `.env` - Environment variables (root)
5. ✅ `infra/.env` - Environment variables (copy for docker-compose)

### Why Two `.env` Files?
- **Root `.env`**: For your reference, version control (with .gitignore)
- **`infra/.env`**: For Docker Compose to read when running services

---

## 🎯 Summary

**Problem:** Docker Compose couldn't find `.env` file  
**Cause:** `.env` was in root, but docker-compose.yml is in `infra/`  
**Solution:** Copied `.env` to `infra/` directory  
**Verification:** All 5 containers running and healthy  

**Time to Fix:** ~3 minutes  
**Downtime:** None (dev environment)  
**Data Lost:** None (volumes preserved)

---

**Next Time:** You'll know exactly where to put the `.env` file! 🚀
