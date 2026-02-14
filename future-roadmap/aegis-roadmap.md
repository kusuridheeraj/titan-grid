# 🚀 Aegis Future Development Roadmap

**Current Version:** 1.0.0 (Production-Ready Rate Limiter)  
**Target:** Aegis 2.0 — The Intelligent, Multi-Tenant, Language-Agnostic API Gateway Shield

---

## 🎯 Vision

Transform Aegis from a Spring Boot-specific rate limiter into a **standalone security platform** that's smarter, more flexible, and usable by any application in any language.

---

## 🔥 Enhancement 1: AI-Powered Adaptive Rate Limiting

### The Problem
Current rate limiters are static. If you set 100 req/min, it stays 100 req/min forever — even when a user's behavior screams "I'm a bot!"

### The Solution
**Behavioral analysis with automatic limit adjustment.**

```java
// Normal user: 100 req/min
// Suspicious pattern detected: Auto-reduce to 10 req/min
// After probation period: Restore to 100 req/min
```

### How It Works

#### Phase 1: Pattern Detection
Track behavioral signals:
- **Request timing variance** — Bots have microsecond precision, humans don't
- **Endpoint diversity** — Real users browse, bots target specific APIs
- **Error rate spikes** — Suddenly hitting 404s? Likely a scanner
- **User-Agent consistency** — Changing UA mid-session is suspicious
- **Geographic jumps** — Tokyo → London in 5 seconds? VPN or bot

#### Phase 2: Scoring System
```java
public class ThreatScore {
    // 0-100 scale
    // 0-30: Normal user
    // 31-60: Watch closely
    // 61-80: Likely bot, reduce limits
    // 81-100: Definitely malicious, strict throttle
}
```

#### Phase 3: Dynamic Limit Adjustment
```sql
-- Real-time rule override
UPDATE rate_limit_rules 
SET limit_count = CASE 
    WHEN threat_score < 30 THEN 100
    WHEN threat_score BETWEEN 31 AND 60 THEN 50
    WHEN threat_score BETWEEN 61 AND 80 THEN 10
    ELSE 5
END
WHERE client_id = 'suspicious-user-123';
```

#### Phase 4: Probation & Recovery
```
Timeline:
- Minute 0: User flagged (threat_score = 75)
- Minute 1-10: Reduced to 10 req/min (probation)
- Minute 11: If behavior improves, restore to 50 req/min
- Minute 21: If still clean, restore to 100 req/min
```

### Implementation Plan

**Tech Stack:**
- **Anomaly Detection:** Redis TimeSeries for real-time behavioral metrics
- **ML Model:** Lightweight on-device model (ONNX Runtime)
  - Input: Last 60 seconds of request patterns
  - Output: Threat score (0-100)
- **Training Data:** PostgreSQL logs from `rate_limit_events` table

**Milestones:**
1. ✅ Week 1: Implement behavioral tracking (timing, diversity, errors)
2. ✅ Week 2: Build simple rule-based scoring (no ML yet)
3. ✅ Week 3: Integrate Redis TimeSeries for real-time metrics
4. ✅ Week 4: Train lightweight TF-Lite model on historical data
5. ✅ Week 5: Deploy model in Java using ONNX Runtime
6. ✅ Week 6: Build auto-recovery system

**Risks:**
- False positives (legitimate users flagged as bots)
- Model drift (attack patterns evolve, model gets stale)

**Mitigation:**
- Manual override API for ops team
- Continuous retraining pipeline
- A/B testing with 10% traffic before full rollout

### Why No Existing Tool Does This

**Resilience4j:** Static rate limiting only, no behavioral analysis  
**Istio:** Config-driven, no runtime intelligence  
**AWS API Gateway:** Offers usage plans, but no adaptive adjustment  

**Aegis 2.0 would be the first to auto-adapt based on behavior.**

---

## 🏢 Enhancement 2: Multi-Tenant Rate Limiting

### The Problem
SaaS platforms have tiers: Free, Pro, Enterprise. Each tier deserves different limits. But managing this across thousands of users is a nightmare.

### The Solution
**Built-in tier-based rate limiting with usage tracking.**

```java
// Free tier: 100 req/min
// Pro tier: 1,000 req/min
// Enterprise tier: 10,000 req/min
// Same user, different APIs, different limits
```

### How It Works

#### Schema: User Subscriptions
```sql
CREATE TABLE aegis.user_subscriptions (
    user_id VARCHAR(255) PRIMARY KEY,
    tier VARCHAR(20) NOT NULL,  -- FREE, PRO, ENTERPRISE
    custom_limits JSONB,         -- Optional overrides
    quota_reset_at TIMESTAMP,
    total_requests_this_month BIGINT DEFAULT 0
);

CREATE TABLE aegis.tier_definitions (
    tier VARCHAR(20) PRIMARY KEY,
    requests_per_minute INT NOT NULL,
    requests_per_day INT,
    requests_per_month INT,
    burst_allowance INT  -- For controlled bursts
);
```

#### Resolver Logic
```java
public RateLimitRule resolveForUser(String userId, String endpoint) {
    // 1. Get user's tier
    Tier tier = subscriptionRepo.findTierByUserId(userId);
    
    // 2. Check for custom overrides (e.g., Enterprise gets custom limits)
    if (tier.hasCustomLimits()) {
        return tier.getCustomLimitForEndpoint(endpoint);
    }
    
    // 3. Apply tier defaults
    return tierDefinitions.get(tier.name());
}
```

#### Usage Tracking
Track monthly quotas in Redis:
```lua
-- Increment monthly usage counter
redis.call('HINCRBY', 'usage:' .. userId .. ':2026-02', 'total', 1)

-- Check if over monthly limit
local monthlyTotal = redis.call('HGET', 'usage:' .. userId .. ':2026-02', 'total')
if tonumber(monthlyTotal) > monthlyLimit then
    return {0, 'Monthly quota exceeded'}
end
```

#### Admin Dashboard
Expose APIs to manage tiers:
```java
POST /api/admin/users/{userId}/upgrade
POST /api/admin/users/{userId}/set-custom-limit
GET  /api/admin/usage/{userId}
```

### Real-World Example

**Scenario:** Your SaaS has 3 tiers

| Tier | Price | Limits |
|------|-------|--------|
| Free | $0 | 100/min, 10K/month |
| Pro | $29/mo | 1,000/min, 1M/month |
| Enterprise | Custom | Custom limits + dedicated support |

**User Journey:**
1. User signs up → Default: Free tier
2. User upgrades to Pro → Limits auto-increase
3. Enterprise sales team negotiates → Custom limits applied via admin API

**Aegis handles it all automatically.**

### Why Resilience4j Can't Do This Elegantly

Resilience4j is annotation-based. To support multi-tenancy, you'd need:
```java
// Ugly workaround (doesn't scale)
@RateLimit(name = "free-tier")
@RateLimit(name = "pro-tier")
@RateLimit(name = "enterprise-tier")
```

And then manually route to the right limiter based on user context. Painful.

**Aegis 2.0:** One config, runtime resolution, database-driven tiers.

---

## 🌐 Enhancement 3: Language-Agnostic Sidecar Deployment

### The Problem
Current Aegis is **tightly coupled to Spring Boot**. If you have a Python app, a Node.js app, or a Go app, you can't use it.

### The Solution
**Deploy Aegis as a standalone HTTP proxy (sidecar pattern).**

```yaml
# ANY app in ANY language can use it
# No code changes needed in your application
```

### Architecture: The Sidecar Pattern

```
┌──────────────────┐
│   Your App       │  (Python/Node/Go/Ruby/Anything)
│   Language: Any  │
└────────┬─────────┘
         │ localhost:8080
         ▼
┌──────────────────┐
│  Aegis Sidecar   │  ← Rate limiting happens here
│  Port: 8081      │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Redis Cluster   │
└──────────────────┘
```

**Flow:**
1. Your app listens on `localhost:8080`
2. Aegis sidecar runs on `localhost:8081`
3. All traffic goes to 8081 first
4. Aegis checks rate limit → Proxies to 8080 if allowed

### How It Works

#### Step 1: Aegis as HTTP Proxy
Rewrite Aegis filter as a standalone Netty-based proxy:

```java
public class AegisProxyServer {
    public static void main(String[] args) {
        NettyHttpServer.create()
            .port(8081)
            .route(routes -> routes
                .filter(new RateLimitFilter())
                .forward("http://localhost:8080")  // Your actual app
            )
            .start();
    }
}
```

#### Step 2: Extract Client ID from Headers
```java
// Standard headers (works with any language)
String clientId = extractFromHeaders(request, List.of(
    "X-API-Key",
    "Authorization",  // JWT extraction
    "X-Forwarded-For" // IP fallback
));
```

#### Step 3: Configuration via Environment Variables
```yaml
# docker-compose.yml
services:
  aegis-sidecar:
    image: titan-grid/aegis:2.0-sidecar
    environment:
      UPSTREAM_URL: http://my-app:8080
      REDIS_URL: redis://redis-cluster:6379
      DEFAULT_LIMIT: 100
      DEFAULT_WINDOW: 60
```

#### Step 4: Kubernetes Sidecar Injection
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: my-app
spec:
  containers:
  - name: my-app
    image: my-company/my-app:latest
    ports:
    - containerPort: 8080
  
  - name: aegis-sidecar  # Auto-injected!
    image: titan-grid/aegis:2.0-sidecar
    ports:
    - containerPort: 8081
    env:
    - name: UPSTREAM_URL
      value: "http://localhost:8080"
```

### Why This Is Better Than Istio

**Istio:**
- ✅ Service mesh, handles routing + rate limiting
- ❌ Heavy (Envoy sidecar = 50MB+ memory overhead)
- ❌ Painful YAML configuration
- ❌ No database-driven rules

**Aegis Sidecar:**
- ✅ Lightweight (20MB memory, focused only on rate limiting)
- ✅ Database-driven (change limits without redeploying)
- ✅ Works with **any** service mesh (or no mesh at all)
- ✅ Simple environment variables for config

### Demo: Protecting a Python Flask App (Zero Code Changes)

**Before Aegis:**
```python
# my_flask_app.py
from flask import Flask
app = Flask(__name__)

@app.route('/api/data')
def get_data():
    return {"message": "Hello"}

if __name__ == '__main__':
    app.run(port=8080)
```

**After Aegis (docker-compose):**
```yaml
services:
  my-flask-app:
    build: .
    ports:
      - "8080"  # Internal only

  aegis-sidecar:
    image: titan-grid/aegis:2.0-sidecar
    ports:
      - "8081:8081"  # External traffic goes here
    environment:
      UPSTREAM_URL: http://my-flask-app:8080
      DEFAULT_LIMIT: 100
```

**Result:** Your Flask app is now rate-limited. Zero code changes. Works the same way for Node, Go, Rust, or any HTTP server.

---

## 🛠️ Implementation Timeline

| Enhancement | Effort | Impact | Priority |
|------------|--------|--------|----------|
| **AI-Powered Adaptive** | 6 weeks | High (differentiates from all competitors) | P1 |
| **Multi-Tenant Support** | 3 weeks | Medium (common SaaS need) | P2 |
| **Language-Agnostic Sidecar** | 4 weeks | High (expands market) | P1 |

**Total:** ~13 weeks (3 months) for Aegis 2.0 release

---

## 📊 Success Metrics

**For v2.0 to be considered successful:**

| Metric | Target |
|--------|--------|
| False positive rate (adaptive) | < 1% |
| Multi-tenant config time | < 5 minutes per tenant |
| Sidecar memory overhead | < 50MB |
| Sidecar latency overhead | < 5ms p99 |
| GitHub stars | 500+ |
| Production deployments | 10+ companies |

---

## 🤝 Contributions Welcome

These enhancements are too big for one person. If you're interested in contributing:

1. **AI/ML Engineers:** Help build the behavioral detection model
2. **SaaS Founders:** Share your multi-tenancy pain points
3. **DevOps Engineers:** Test the sidecar pattern in real Kubernetes deployments

Reach out: [LinkedIn](https://linkedin.com/in/kusuridheeraj) or open an issue on GitHub.

---

## 📝 Conclusion

**Aegis 1.0** solved distributed rate limiting with atomic operations and hybrid configuration.

**Aegis 2.0** will solve:
- **Adaptive security** (AI-powered)
- **SaaS scalability** (multi-tenant)
- **Universal compatibility** (language-agnostic sidecar)

No existing tool does all three. That's the gap we're filling.

---

*This roadmap is a living document. Last updated: February 14, 2026*
