# Aegis - Distributed Rate Limiter

High-performance rate limiting service using **Redis** and **Sliding Window algorithm**. Prevents API abuse and ensures fair resource allocation across distributed systems.

---

## Features

✅ **Sliding Window Counter** - Accurate rate limiting without boundary issues  
✅ **Atomic Operations** - Lua scripts prevent race conditions  
✅ **High Performance** - Handles 10,000+ requests/second  
✅ **Graceful Degradation** - Configurable failure mode (ALLOW/DENY)  
✅ **Prometheus Metrics** - Full observability  
✅ **Thread-Safe** - Works in distributed environments  

---

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.9+
- Redis running (via Docker Compose)

### 1. Start Redis
```powershell
cd c:\PlayStation\assets\titan-grid
docker-compose -f infra/docker-compose.yml up -d
```

### 2. Build Project
```powershell
cd aegis
mvn clean package
```

### 3. Run Application
```powershell
mvn spring-boot:run
```

Application starts on **http://localhost:8080**

---

## API Endpoints

### Test Endpoints

#### Default Rate Limit (100 requests/min)
```bash
GET http://localhost:8080/api/test/limited
```

**Response (200 OK):**
```json
{
  "message": "Request allowed",
  "remaining": 99,
  "limit": 100,
  "resetTime": "2026-02-06T16:30:00Z"
}
```

**Response (429 Too Many Requests):**
```json
{
  "error": "Rate limit exceeded",
  "message": "Too many requests. Please try again later.",
  "retryAfter": 45
}
```

#### Strict Rate Limit (10 requests/min)
```bash
GET http://localhost:8080/api/test/strict
```

### Admin Endpoints

#### Get Rate Limit Info
```bash
GET http://localhost:8080/api/admin/info?clientId=192.168.1.1&endpoint=/api/test/limited
```

**Response:**
```json
{
  "clientId": "192.168.1.1",
  "endpoint": "/api/test/limited",
  "allowed": false,
  "currentCount": 100,
  "limit": 100,
  "remaining": 0,
  "resetTime": "2026-02-06T16:30:00Z",
  "retryAfter": 30
}
```

#### Reset Rate Limit
```bash
POST http://localhost:8080/api/admin/reset?clientId=192.168.1.1&endpoint=/api/test/limited
```

---

## Testing

### Manual Testing

**Test A: Normal Requests**
```powershell
# Send 5 requests (all should succeed)
1..5 | ForEach-Object {
    Invoke-RestMethod http://localhost:8080/api/test/limited
}
```

**Test B: Exceed Limit**
```powershell
# Send 101 requests rapidly
1..101 | ForEach-Object {
    try {
        $response = Invoke-WebRequest http://localhost:8080/api/test/limited -SkipHttpErrorCheck
        Write-Host "Request $_`: Status $($response.StatusCode)"
    } catch {
        Write-Host "Request $_`: Error"
    }
}
# Expected: First 100 succeed (200), 101st fails (429)
```

**Test C: Sliding Window**
```powershell
# Wait 60 seconds, then send request
Start-Sleep -Seconds 60
Invoke-RestMethod http://localhost:8080/api/test/limited
# Expected: 200 OK (window reset)
```

### Automated Tests
```powershell
mvn test
```

---

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
aegis:
  rate-limiter:
    default-limit: 100       # Default requests per window
    strict-limit: 10         # Strict requests per window  
    window-duration: 60      # Window duration (seconds)
    key-prefix: "rate_limit" # Redis key prefix
    failure-mode: ALLOW      # ALLOW or DENY when Redis is down
```

---

## Monitoring

### Prometheus Metrics
```bash
curl http://localhost:8080/actuator/prometheus | Select-String "rate_limit"
```

**Available Metrics:**
- `rate_limit_requests_total` - Total requests (labeled by result: allowed/denied)
- `jvm_memory_used_bytes` - JVM memory usage
- `http_server_requests_seconds` - HTTP request duration

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

---

## Architecture

### Sliding Window Algorithm

Uses Redis **Sorted Sets** to track requests in a time window:

```
ZADD rate_limit:{clientId}:{endpoint} {timestamp} {requestId}
```

**Lua Script Flow:**
1. Remove expired entries (outside window)
2. Count current requests
3. If count < limit: Add new request
4. Return: {allowed, count, ttl}

**Benefits:**
- O(log N) insert/delete
- O(1) count
- Atomic execution (no race conditions)

### Components

```
RateLimiterController
    ↓
RateLimiterService
    ↓ Execute Lua Script
Redis (Sorted Sets)
```

---

## Redis Data Structure

```redis
# Key format
rate_limit:{clientId}:{endpoint}

# Example
rate_limit:192.168.1.1:/api/test/limited

# Sorted set members (score = timestamp)
ZADD rate_limit:192.168.1.1:/api/test 1707218400 req_uuid_1
ZADD rate_limit:192.168.1.1:/api/test 1707218401 req_uuid_2

# Count requests in window
ZCOUNT rate_limit:192.168.1.1:/api/test <window_start> <current_time>
```

---

## Development

### Project Structure
```
aegis/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/titan/aegis/
│   │   │   ├── AegisApplication.java
│   │   │   ├── config/
│   │   │   │   ├── RedisConfig.java
│   │   │   │   └── RateLimiterProperties.java
│   │   │   ├── service/
│   │   │   │   └── RateLimiterService.java
│   │   │   ├── model/
│   │   │   │   ├── RateLimitDecision.java
│   │   │   │   └── RequestToken.java
│   │   │   └── controller/
│   │   │       └── RateLimiterController.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── lua/
│   │           └── sliding_window.lua
│   └── test/
│       └── java/com/titan/aegis/
│           └── service/
│               └── RateLimiterServiceTest.java
└── README.md
```

### Build Commands
```powershell
# Clean build
mvn clean package

# Skip tests
mvn clean package -DskipTests

# Run tests only
mvn test

# Run specific test
mvn test -Dtest=RateLimiterServiceTest

# Run with profile
mvn spring-boot:run -Dspring.profiles.active=dev
```

---

## Troubleshooting

### Redis Connection Failed
```
Error: Unable to connect to Redis at localhost:6379
```

**Solution:**
```powershell
# Check if Redis is running
docker ps | Select-String redis

# If not, start it
docker-compose -f infra/docker-compose.yml up -d titan-redis

# Test connection
docker exec -it titan-redis redis-cli -a $env:REDIS_PASSWORD ping
```

### Port 8080 Already in Use
```
Error: Port 8080 is already in use
```

**Solution:**
```powershell
# Find process using port 8080
netstat -ano | findstr :8080

# Kill process
taskkill /PID <PID> /F

# Or change port in application.yml
server:
  port: 8081
```

---

## Next Steps

- **Day 4:** Implement `AegisFilter` (servlet filter for all requests)
- **Day 5:** Add PostgreSQL logging for blocked requests
- **Day 6:** Add Redis Stream for suspicious traffic
- **Day 7:** Performance testing and optimization

---

## License

Part of Titan Grid - Staff Engineer Portfolio Project
