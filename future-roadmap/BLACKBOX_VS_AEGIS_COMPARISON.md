# 🆚 BLACKBOX vs AEGIS: Tale of Two Rate Limiters

**Author:** Dheeraj Kusuri  
**Date:** February 14, 2026  
**Purpose:** Comparative analysis of two distinct rate limiting implementations

---

## Executive Summary

You've built two rate limiting systems that **solve different problems with different approaches**. They're not duplicates — they're complementary explorations of the same design space.

| Dimension | BLACKBOX | AEGIS |
|-----------|----------|-------|
| **Primary Goal** | API Gateway with self-healing | Distributed rate limiter library |
| **Algorithm** | Token Bucket | Sliding Window |
| **Unique Feature** | Adaptive limits based on backend health | 3-tier hybrid configuration |
| **Deployment** | Standalone gateway (proxy) | Spring Boot library/filter |
| **Best For** | Protecting backends from overload | Enforcing strict API quotas |

**Bottom Line:** BLACKBOX is a **reactive protection system**. AEGIS is a **proactive enforcement system.**

---

## 🧬 Core Architecture Comparison

### BLACKBOX: API Gateway Pattern

```
Client → JWT Auth → Rate Limit → Circuit Breaker → Backend
                                       ↑
                              Adaptive Controller
                         (monitors backend health)
```

**Philosophy:** "The backend is struggling. Let's throttle traffic automatically."

**Components:**
- `RateLimitFilter` — Token bucket enforcement
- `AdaptiveRateLimitController` — Self-healing brain (every 10 seconds)
- `CircuitBreaker` — Per-route failure detection
- `RequestRouter` — Proxies to backend
- `JwtAuthFilter` — Edge authentication

### AEGIS: Library/Filter Pattern

```
Client → AegisFilter → Rate Limit Decision → Your Controller
                            ↑
                    RateLimitRuleResolver
                 (Priority: Annotation > DB > YAML)
```

**Philosophy:** "This client exceeded their quota. Block them, no exceptions."

**Components:**
- `AegisFilter` — Servlet filter (before controller)
- `RateLimitRuleResolver` — 3-tier config merger
- `RateLimiterService` — Sliding window logic (Lua)
- `ClientIdExtractor` — Multi-tier client identification
- `RateLimitEventPublisher` — PostgreSQL + Redis Stream logging

---

## 🎯 Algorithm Choice: Token Bucket vs Sliding Window

### BLACKBOX: Token Bucket

```lua
-- Simplified Redis Lua script
local tokens = redis.call('GET', key) or maxTokens
local now = ARGV[3]

if tokens > 0 then
    redis.call('DECR', key)
    return 1  -- Allowed
else
    return 0  -- Denied
end
```

**Characteristics:**
- ✅ Allows bursts (e.g., 75 requests in 1 second if bucket is full)
- ✅ Gradual refill (50 tokens/sec = 1 token every 20ms)
- ✅ Better UX (responsive for legitimate bursts)
- ❌ Can be gamed at bucket boundary
- ❌ Less strict enforcement

**Why Token Bucket for BLACKBOX?**
- Gateway serves **mixed traffic** (legitimate bursts from valid users)
- UX matters (users shouldn't feel the throttling)
- Backend protection is priority, not quota enforcement

### AEGIS: Sliding Window

```lua
-- Simplified Redis Lua script
redis.call('ZREMRANGEBYSCORE', key, 0, now - windowSize)  -- Clean old
local count = redis.call('ZCARD', key)                     -- Count current

if count < limit then
    redis.call('ZADD', key, now, requestId)  -- Allow + record
    return {1, count + 1, limit}
else
    return {0, count, limit}                  -- Block
end
```

**Characteristics:**
- ✅ **No bursts possible** (100/min = max 100 in ANY 60-second window)
- ✅ Strict quota enforcement
- ✅ Unforgeable (can't game the boundary)
- ❌ Higher memory (stores individual timestamps)
- ❌ Less forgiving (no "burst allowance")

**Why Sliding Window for AEGIS?**
- Designed for **API security** (prevent DDoS, abuse)
- Strict quotas matter (SaaS billing, compliance)
- No bursts = no attack vectors

---

## 🛡️ Unique Features

### BLACKBOX's Killer Feature: Adaptive Limits

```java
// Every 10 seconds, evaluate backend health
@Scheduled(fixedRate = 10_000)
public void evaluateAndAdjust() {
    double errorRate = (double) errors / total;
    
    if (errorRate > 0.50) {
        // Backend is dying — cut traffic in half
        currentMultiplier = 0.5;  // TIGHTENED
        
    } else if (errorRate > 0.20) {
        // Backend is stressed — reduce by 20%
        currentMultiplier = 0.8;  // CAUTIOUS
        
    } else if (errorRate < 0.05) {
        // Backend healthy for 2 minutes — restore
        currentMultiplier = 1.0;  // NORMAL
    }
}
```

**State Machine:**

```
NORMAL (1.0x) → CAUTIOUS (0.8x) → TIGHTENED (0.5x) → RECOVERING (gradual) → NORMAL
```

**Impact:**
- **Self-healing** — No manual intervention during incidents
- **Fail-safe** — Protects backend before circuit breaker opens
- **Better UX** — Throttles gradually instead of all-or-nothing circuit breaking

**No equivalent in AEGIS.** Aegis has **static limits** (can be changed via database, but not *automatically* based on system health).

---

### AEGIS's Killer Feature: 3-Tier Hybrid Configuration

```java
// Priority resolution
public RateLimitRule resolveRule(String endpoint, ClientType clientType) {
    // Priority 1: Annotation (developer says "this endpoint needs 5/min")
    RateLimitRule annotationRule = getAnnotationRule(endpoint);
    if (annotationRule != null) return annotationRule;
    
    // Priority 2: Database (ops says "we're under attack, tighten to 10/min")
    RateLimitRule dbRule = getDatabaseRule(endpoint);
    if (dbRule != null) return dbRule;
    
    // Priority 3: YAML (default fallback: 100/min)
    return getYamlDefault();
}
```

**Tiers:**

| Priority | Source | Use Case | Example |
|----------|--------|----------|---------|
| 1 (Highest) | `@RateLimit` annotation | Security-critical endpoints | `@RateLimit(limit=5)` on login |
| 2 (Medium) | Database rules | Runtime adjustments | Update DB during attack, no restart |
| 3 (Lowest) | YAML config | Global defaults | 100 req/min for everything else |

**Impact:**
- **Zero-downtime config changes** (update DB, wait 60 sec for cache)
- **Developer control** (annotation = "this MUST be 5/min, don't override")
- **Ops flexibility** (database = "temporary tightening during incident")

**No equivalent in BLACKBOX.** Blackbox has **tier-based limits** (STANDARD/PREMIUM/INTERNAL) but they're static in `application.yml`, no runtime changes without restart.

---

## 📊 Configuration Systems

### BLACKBOX: Tier-Based (JWT Claims)

```yaml
# application.yml
gateway:
  rate-limit:
    tiers:
      STANDARD:
        requests-per-second: 50
        burst-size: 75
      PREMIUM:
        requests-per-second: 500
        burst-size: 750
      INTERNAL:
        requests-per-second: 1000
        burst-size: 1500
```

```java
// JWT contains tier claim
{
  "clientId": "user-123",
  "tier": "PREMIUM",  ← This determines rate limit
  "name": "Acme Corp"
}
```

**How it works:**
1. Client presents JWT token
2. Gateway extracts `tier` claim
3. Looks up tier config in memory
4. Applies token bucket with tier's limits

**Pros:**
- Simple and fast (no database lookup)
- Tier in token = can't be manipulated by client
- Good for multi-tenant SaaS

**Cons:**
- Changing limits requires restart (YAML change + redeploy)
- No per-user overrides (all PREMIUM users get same limit)

---

### AEGIS: 3-Tier Hybrid

```java
// Priority 1: Annotation
@RateLimit(limit = 5, windowSeconds = 60)
public String login() { ... }

// Priority 2: Database (runtime-configurable)
INSERT INTO aegis.rate_limit_rules (endpoint_pattern, limit_count)
VALUES ('/api/search/**', 50);

// Priority 3: YAML (fallback)
rate-limiter:
  default-limit: 100
```

**How it works:**
1. Request arrives at `/api/search/users`
2. Check annotation on the controller method → None found
3. Query database for pattern `/api/search/**` → Match! (50/min)
4. Use that limit

**Pros:**
- **Runtime changes** (update DB, no restart)
- **Developer safety** (annotation overrides everything)
- **Ant path patterns** (`/api/**`, `/admin/**`) for flexible matching

**Cons:**
- Database query on first request (cached for 60 sec)
- More complex (3 sources of truth to reason about)

---

## 🔧 Deployment Models

### BLACKBOX: Standalone Gateway

```yaml
# docker-compose.yml
services:
  blackbox-gateway:
    image: blackbox:latest
    ports:
      - "8080:8080"
    environment:
      BACKEND_URL: http://my-app:8081
      
  my-app:
    image: my-company/my-app:latest
    ports:
      - "8081"  # Internal only
```

**Gateway Pattern:**
- Client talks to gateway on port 8080
- Gateway forwards to backend on port 8081
- Backend doesn't know about rate limiting

**Pros:**
- ✅ **Language-agnostic** (backend can be Python/Node/Go/anything)
- ✅ **Centralized** (one place to configure all limits)
- ✅ **No code changes** to your app

**Cons:**
- ❌ Extra network hop (gateway → backend latency)
- ❌ Single point of failure (if gateway dies, app is unreachable)

---

### AEGIS: Embedded Library (Spring Boot Filter)

```java
// Your Spring Boot app includes Aegis as a dependency
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        // AegisFilter auto-registers on startup
        SpringApplication.run(MyApplication.class, args);
    }
}
```

**Filter Pattern:**
- Client talks directly to your app
- `AegisFilter` intercepts request **before** controller
- No separate gateway process

**Pros:**
- ✅ **Zero latency** (no extra hop)
- ✅ **Easier deployment** (single JAR file)
- ✅ **Fine-grained control** (per-endpoint annotations)

**Cons:**
- ❌ **Spring Boot only** (can't use with Python/Node apps)
- ❌ **Embedded** (every app needs to include Aegis)

---

## 🚨 Failure Behavior

### BLACKBOX: Graceful Degradation

| Component Down | Behavior | Client Impact |
|---------------|----------|---------------|
| **Redis** | Local in-memory counter at 50% capacity | More conservative throttling |
| **PostgreSQL** | Audit logs fail silently | None |
| **Backend** | Circuit breaker opens → 503 response | Service unavailable |

**Design Principle:** "Degrade, don't crash."

```java
try {
    return tryAcquireFromRedis(identity, routeId);
} catch (Exception e) {
    log.warn("Redis unavailable, using local fallback");
    return tryAcquireLocal(identity, routeId);  // 50% capacity
}
```

---

### AEGIS: Fail-Open

| Component Down | Behavior | Client Impact |
|---------------|----------|---------------|
| **Redis** | Allow all requests | Throttling disabled (fail-open) |
| **PostgreSQL** | Event logging disabled | None |

**Design Principle:** "Availability > perfect security."

```java
try {
    return rateLimiterService.tryAcquire(clientId, endpoint);
} catch (Exception e) {
    log.error("Rate limiter failed, allowing request", e);
    return RateLimitDecision.allowed("failopen");  // Let it through
}
```

**Trade-off:** Temporary abuse spike vs total service outage.

---

## 📈 Observability Comparison

### BLACKBOX Metrics

```
gateway_request_total{route="/api/test", status="200"}
gateway_rate_limit_throttled_total{route="/api/test", tier="STANDARD"}
gateway_circuit_breaker_state{route="/api/backend"} = 0 (CLOSED)
gateway_adaptive_adjustment_total{direction="tighten"}
gateway_fallback_total{component="redis"}
```

**Dashboard:** Prometheus + Grafana (7 panels), shows circuit breaker state, adaptive adjustments

---

### AEGIS Metrics

```
aegis_requests_total{endpoint="/api/test", status="allowed"}
aegis_requests_blocked_total{endpoint="/api/test", client_type="IP"}
aegis_lua_execution_duration_seconds{quantile="0.95"}
aegis_rule_cache_hit_rate
aegis_stream_events_published_total{severity="HIGH"}
```

**Dashboard:** Prometheus + Grafana (9 panels), shows rate limit trends, Redis Stream alerts

---

## 🎓 Learning Value

### What BLACKBOX Taught You

1. **API Gateway pattern** — How Kong/Tyk/AWS API Gateway work internally
2. **Circuit breakers** — Resilience patterns (CLOSED → OPEN → HALF_OPEN)
3. **Adaptive systems** — Heuristics-based self-healing (vs static rules)
4. **Token bucket algorithm** — Better UX, allows bursts
5. **JWT at the edge** — Authentication before processing

### What AEGIS Taught You

1. **Distributed correctness** — Lua atomicity, no race conditions
2. **Sliding window algorithm** — Strict security, no burst exploits
3. **Hybrid configuration** — Priority systems (annotation > DB > YAML)
4. **Redis Sorted Sets** — Efficient timestamp-based windowing
5. **Multi-tier observability** — PostgreSQL + Redis Stream + Prometheus

---

## ⚖️ When to Use Each

### Use BLACKBOX When...

✅ You need an **API gateway** (centralized routing point)  
✅ You have **multiple backends** in different languages  
✅ You want **automatic circuit breaking + rate limiting**  
✅ You need **adaptive limits** that adjust to backend health  
✅ You're building a **multi-tenant SaaS** with tier-based pricing  

**Example:** You have 5 microservices (Python, Node, Java, Go) and want one gateway to protect them all.

---

### Use AEGIS When...

✅ You need **strict quota enforcement** (SaaS billing, compliance)  
✅ You want **zero bursts** (security-first design)  
✅ You need **runtime config changes** without restart (database rules)  
✅ You're building a **Spring Boot microservice** (not a gateway)  
✅ You want **fine-grained control** (per-endpoint annotations)  

**Example:** You're building a public API with strict quotas (100 req/min for free tier, no exceptions).

---

## 🔄 Can They Work Together?

**Yes!** Here's a killer combo:

```
        ┌──────────────┐
Client  │   BLACKBOX   │  ← API Gateway (adaptive, circuit breaking)
   ↓    │   (Gateway)  │
        └──────┬───────┘
               │
        ┌──────▼───────┐
        │    AEGIS     │  ← Embedded in your Spring Boot app (strict quotas)
        │   (Filter)   │
        └──────┬───────┘
               │
        ┌──────▼───────┐
        │  Your App    │
        │  (Business   │
        │   Logic)     │
        └──────────────┘
```

**Why this works:**
- **BLACKBOX** handles adaptive protection (if backend is stressed, reduce ALL traffic)
- **AEGIS** handles per-user quotas (user X gets 100/min, user Y gets 1000/min)
- **Two layers of defense** — Gateway-level + App-level

---

## 📝 Similarities (What Both Do Well)

| Feature | BLACKBOX | AEGIS |
|---------|----------|-------|
| **Redis-backed** | ✅ | ✅ |
| **Lua atomic operations** | ✅ | ✅ |
| **Prometheus metrics** | ✅ | ✅ |
| **Grafana dashboards** | ✅ | ✅ |
| **PostgreSQL audit logs** | ✅ | ✅ |
| **Multi-instance safe** | ✅ | ✅ |
| **Fail gracefully** | ✅ (local fallback) | ✅ (fail-open) |
| **Production-ready** | ✅ | ✅ |

---

## 🔥 Key Differences Summary

| Dimension | BLACKBOX | AEGIS |
|-----------|----------|-------|
| **Algorithm** | Token Bucket (bursts allowed) | Sliding Window (no bursts) |
| **Deployment** | Standalone gateway | Embedded filter |
| **Config** | YAML tiers (static) | Annotation + DB + YAML (hybrid) |
| **Unique Feature** | Adaptive limits (self-healing) | 3-tier priority config |
| **Best For** | Backend protection, multi-service | Strict quotas, single service |
| **Language Support** | Any (gateway pattern) | Spring Boot only |
| **Runtime Changes** | No (requires restart) | Yes (database rules) |
| **UX Philosophy** | Forgiving (allow bursts) | Strict (security-first) |
| **Failure Mode** | Local fallback (50%) | Fail-open (allow all) |

---

## 💡 Interview Story You Can Tell

**Interviewer:** "I see you built two rate limiters. Isn't that redundant?"

**You:** "Not at all. They solve different problems with different trade-offs."

**BLACKBOX** is like a **smart traffic cop**. If the highway ahead (backend) is jammed, it automatically slows down incoming traffic. It's adaptive — it watches the system and adjusts in real-time. Great for protecting backends.

**AEGIS** is like a **strict bouncer**. You're on the free tier? Maximum 100 requests per hour, period. No bursts, no exceptions. It's designed for API security and quota enforcement. Great for SaaS billing.

**The algorithm choice reflects this:**
- BLACKBOX uses **Token Bucket** (allows bursts for better UX)
- AEGIS uses **Sliding Window** (no bursts for security)

**And the deployment model:**
- BLACKBOX is a **standalone gateway** (works with any language)
- AEGIS is an **embedded library** (Spring Boot only, but zero latency)

**I built both because I wanted to understand the full design space.** In a real system, I might even use **both** — BLACKBOX at the edge for adaptive protection, AEGIS inside each service for strict quotas.

---

## 🎯 Bottom Line

| Question | Answer |
|----------|--------|
| Are they duplicates? | **No.** Different algorithms, architectures, and use cases. |
| Which is better? | **Depends.** BLACKBOX for gateways, AEGIS for strict quotas. |
| Can you use both? | **Yes!** They're complementary (gateway + app-level). |
| What's the learning value? | **Huge.** You explored the entire rate limiting design space. |

---

## 📚 What This Proves to Employers

1. **You understand trade-offs** — Token Bucket vs Sliding Window
2. **You can architecture multiple solutions** — Gateway vs Library pattern
3. **You think about production** — Adaptive limits, graceful degradation
4. **You're not just copying tutorials** — Each project has unique innovations
5. **You can explain your choices** — Why this algorithm for this problem

---

**Final Verdict:** Keep both projects in your portfolio. They tell a complete story about distributed systems, rate limiting algorithms, and production-grade engineering.

---

*This comparison was written to help you articulate the value of both projects during interviews. Use it as a reference when explaining your design decisions.*

*— Dheeraj, reflecting on your engineering journey*
