# ✅ Day 2-3 Completion Checklist

## 📦 What We've Built

### Core Files Created
- ✅ [pom.xml](file:///c:/PlayStation/assets/titan-grid/aegis/pom.xml) - Maven dependencies
- ✅ [application.yml](file:///c:/PlayStation/assets/titan-grid/aegis/src/main/resources/application.yml) - Configuration
- ✅ [sliding_window.lua](file:///c:/PlayStation/assets/titan-grid/aegis/src/main/resources/lua/sliding_window.lua) - Atomic rate limit script
- ✅ [AegisApplication.java](file:///c:/PlayStation/assets/titan-grid/aegis/src/main/java/com/titan/aegis/AegisApplication.java) - Main class
- ✅ [RateLimiterService.java](file:///c:/PlayStation/assets/titan-grid/aegis/src/main/java/com/titan/aegis/service/RateLimiterService.java) - Core service
- ✅ [RateLimiterController.java](file:///c:/PlayStation/assets/titan-grid/aegis/src/main/java/com/titan/aegis/controller/RateLimiterController.java) - REST API
- ✅ [RedisConfig.java](file:///c:/PlayStation/assets/titan-grid/aegis/src/main/java/com/titan/aegis/config/RedisConfig.java) - Redis setup
- ✅ [RateLimiterProperties.java](file:///c:/PlayStation/assets/titan-grid/aegis/src/main/java/com/titan/aegis/config/RateLimiterProperties.java) - Properties
- ✅ [RateLimitDecision.java](file:///c:/PlayStation/assets/titan-grid/aegis/src/main/java/com/titan/aegis/model/RateLimitDecision.java) - Response model
- ✅ [RequestToken.java](file:///c:/PlayStation/assets/titan-grid/aegis/src/main/java/com/titan/aegis/model/RequestToken.java) - Request model
- ✅ [README.md](file:///c:/PlayStation/assets/titan-grid/aegis/README.md) - Complete documentation

---

## 🚀 Current Status

**Maven Build:** ✅ SUCCESS (`mvn clean compile`)  
**Spring Boot:** 🟡 RUNNING (needs password in `.env` to connect to Redis)  
**Redis:** ✅ RUNNING (titan-redis container up)  

---

## ⚠️ Before Testing - Update Passwords

The application is running but **can't connect to Redis** because passwords in `.env` are still placeholders.

### 1. Edit `.env` File

Edit `c:\PlayStation\assets\titan-grid\.env`:
```env
# Change from:
REDIS_PASSWORD=CHANGE_THIS_PASSWORD_NOW

# To (use any password you want):
REDIS_PASSWORD=my_secure_redis_password_123
```

### 2. Edit `infra/.env` File

Copy the same password to `c:\PlayStation\assets\titan-grid\infra\.env`:
```env
REDIS_PASSWORD=my_secure_redis_password_123
```

### 3. Restart Redis with New Password

```powershell
cd c:\PlayStation\assets\titan-grid
docker-compose -f infra/docker-compose.yml down
docker-compose -f infra/docker-compose.yml up -d redis
```

### 4. Stop and Restart Aegis

Press `Ctrl+C` in the terminal running Aegis, then:
```powershell
cd aegis
mvn spring-boot:run
```

---

## 🧪 Testing Steps

Once Aegis is running with correct Redis password:

### Step 1: Verify Application Started
Look for this in console:
```
Started AegisApplication in X.XXX seconds
Tomcat started on port(s): 8080 (http)
```

### Step 2: Test Normal Requests
```powershell
# Send 5 requests (all should succeed with 200 OK)
1..5 | ForEach-Object {
    $response = Invoke-RestMethod http://localhost:8080/api/test/limited
    Write-Host "Request $_`: $($response.message), Remaining: $($response.remaining)"
}
```

**Expected Output:**
```
Request 1: Request allowed, Remaining: 99
Request 2: Request allowed, Remaining: 98
Request 3: Request allowed, Remaining: 97
Request 4: Request allowed, Remaining: 96
Request 5: Request allowed, Remaining: 95
```

### Step 3: Test Rate Limiting
```powershell
# Send 101 requests rapidly
1..101 | ForEach-Object {
    try {
        $response = Invoke-WebRequest http://localhost:8080/api/test/limited -SkipHttpErrorCheck
        if ($response.StatusCode -eq 200) {
            Write-Host "Request $_`: ALLOWED" -ForegroundColor Green
        } else {
            Write-Host "Request $_`: BLOCKED (429)" -ForegroundColor Red
        }
    } catch {
        Write-Host "Request $_`: ERROR" -ForegroundColor Yellow
    }
}
```

**Expected:**
- Requests 1-100: `ALLOWED` (green)
- Request 101: `BLOCKED (429)` (red)

### Step 4: Check Rate Limit Info
```powershell
# Get current rate limit status
curl http://localhost:8080/api/admin/info?clientId=127.0.0.1&endpoint=/api/test/limited
```

**Expected Response:**
```json
{
  "clientId": "127.0.0.1",
  "endpoint": "/api/test/limited",
  "allowed": false,
  "currentCount": 100,
  "limit": 100,
  "remaining": 0,
  "resetTime": "2026-02-06T16:30:00Z",
  "retryAfter": 30
}
```

### Step 5: Reset Rate Limit
```powershell
# Reset to test again
curl -X POST http://localhost:8080/api/admin/reset?clientId=127.0.0.1&endpoint=/api/test/limited
```

### Step 6: Test Strict Limit (10 req/min)
```powershell
# Send 11 requests to strict endpoint
1..11 | ForEach-Object {
    try {
        $response = Invoke-WebRequest http://localhost:8080/api/test/strict -SkipHttpErrorCheck
        Write-Host "Request $_`: Status $($response.StatusCode)"
    } catch {
        Write-Host "Request $_`: ERROR"
    }
}
```

**Expected:**
- Requests 1-10: `Status 200`
- Request 11: `Status 429`

### Step 7: Verify Prometheus Metrics
```powershell
# Check metrics endpoint
curl http://localhost:8080/actuator/prometheus
```

Look for:
```
rate_limit_requests_total{result="allowed"} 100.0
rate_limit_requests_total{result="denied"} 1.0
```

### Step 8: Verify Redis Data
```powershell
# Check Redis for rate limit keys
docker exec -it titan-redis redis-cli -a your-password-here

# In Redis CLI:
KEYS rate_limit:*
ZRANGE rate_limit:127.0.0.1:/api/test/limited 0 -1 WITHSCORES
```

**Expected:** You'll see sorted set entries with timestamps

---

## ✅ Completion Criteria

Day 2-3 is complete when:
- [x] Project compiles successfully ✅
- [ ] Application starts without errors (needs password fix)
- [ ] Redis connection established
- [ ] 100 requests succeed, 101st returns HTTP 429
- [ ] Sliding window: Old requests expire after 60 seconds
- [ ] Admin endpoints work (info, reset)
- [ ] Prometheus metrics showing correct counts
- [ ] Redis contains rate limit sorted sets

---

## 🎯 What's Next?

**Day 4-5: HTTP Filter Chain**
- Create `AegisFilter` (servlet filter)
- Apply rate limiting to ALL requests automatically
- Extract client ID from headers (X-API-Key, X-Forwarded-For)
- Integrate PostgreSQL logging for blocked requests
- Push suspicious traffic to Redis Stream

**When ready, message:**
> "Day 2-3 complete! Ready for Day 4"

---

## 🐛 Known Issues

### Issue 1: REDIS_PASSWORD Not Set
**Symptom:** Application logs show "Unable to connect to Redis"  
**Fix:** Update `.env` and `infra/.env` with actual password, restart Redis and Aegis

### Issue 2: Port 8080 Already in Use
**Symptom:** "Port 8080 is already in use"  
**Fix:** 
```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Issue 3: Lua Script Not Found
**Symptom:** "FileNotFoundException: lua/sliding_window.lua"  
**Fix:** Verify file exists at `aegis/src/main/resources/lua/sliding_window.lua`

---

**Current Status:** Code complete ✅ | Needs password configuration 🔧 | Ready to test 🚀
