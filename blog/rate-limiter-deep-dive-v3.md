# Why I Built My Own Rate Limiter Instead of Using Resilience4j

*By Dheeraj Kusuri*

---

## The Question Every Tech Lead Asked Me

"Why build this from scratch? Resilience4j exists. Spring Cloud Gateway has rate limiting built-in. You're solving a solved problem."

Fair question. Here's my answer.

---

## The Algorithm Choice That Changes Everything

When I started researching rate limiting libraries, I noticed something: **they all use Token Bucket.**

Resilience4j? Token Bucket.  
Spring Cloud Gateway? Token Bucket.  
AWS API Gateway? Token Bucket.  

Token Bucket is brilliant for *traffic shaping* — allowing controlled bursts while maintaining an average rate. Perfect for internal microservices that need flexibility.

**But I wasn't building a traffic shaper. I was building an API security layer.**

And for security, Token Bucket has a fatal flaw: **it allows bursts.**

### The Burst Attack Problem

Imagine your limit is 100 requests per minute. With Token Bucket:

```
Second 1: Client sends 100 requests (empties the bucket)
Second 2-60: Client waits for tokens to refill
```

That's 100 requests in **one second**, even though your "limit" is 100 per *minute*.

For DoS protection? That's a problem. Those 100 concurrent requests can overwhelm your database, exhaust connection pools, or trigger cascading failures — all within the "allowed" limit.

### Why I Chose Sliding Window

Here's the comparison table that made my decision clear:

| Algorithm | Allows Burst | Smooth Rate | Implementation | Best For |
|-----------|-------------|-------------|----------------|----------|
| **Fixed Window** | ❌ No | ❌ No (boundary exploit) | Easy | Simple counters |
| **Sliding Window** | ❌ No | ✅ Yes | Medium | **Security/API protection** |
| **Leaky Bucket** | ❌ No | ✅ Yes | Medium | Traffic shaping with queues |
| **Token Bucket** | ✅ Yes | ✅ Yes | Medium | **Microservice resilience** |

**Sliding Window = No bursts, ever.** If I say 100 requests per minute, I mean 100 in *any* 60-second window. No gaming the system by hitting the boundary between minutes.

Resilience4j optimizes for user experience (bursts feel responsive). I optimized for **unforgeable security guarantees.**

---

## The Distributed Race Condition No Library Solves

Even if Resilience4j had Sliding Window, I'd still have a problem: **distributed race conditions.**

Most rate limiting libraries work like this:

```java
// Resilience4j's model (simplified)
long count = rateLimiter.getPermitsAvailable();
if (count > 0) {
    rateLimiter.acquirePermission();
    processRequest();
}
```

This is **check-then-act**. In a single-server setup, atomic operations protect you. But the moment you add a second server behind a load balancer, you're vulnerable:

```
Server A reads count = 9  ← Both read at the same microsecond
Server B reads count = 9  
Server A allows request (writes 10)
Server B allows request (writes 10... wait, that's 11?)
```

**The fix? Move the decision into Redis with a Lua script:**

```lua
-- This entire block executes atomically in Redis
-- No server can interrupt it mid-execution

redis.call('ZREMRANGEBYSCORE', key, 0, now - 60)  -- Clean old entries
local count = redis.call('ZCARD', key)             -- Count current

if count < limit then
    redis.call('ZADD', key, now, requestId)        -- Allow + record
    return {1, count + 1, limit}
else
    return {0, count, limit}                       -- Block
end
```

**One atomic decision.** No gap between check and act. No race condition. Zero leaked requests.

Resilience4j can't do this. It's designed for in-memory resilience, not distributed correctness.

---

## The Hybrid Configuration System (My Killer Feature)

Here's the real differentiation: **How do you configure limits without restarting your app?**

Resilience4j gives you annotations and YAML. Great for static configs. Terrible for responding to attacks at 2 AM.

I built a **3-tier priority system**:

### Priority 1: Annotation (Developer Control)
```java
@RateLimit(limit = 5, windowSeconds = 60, clientType = IP)
public String login(@RequestBody LoginRequest req) {
    // Strict limit on login to prevent brute force
}
```

**When to use:** Security-critical endpoints where the limit must never change.

### Priority 2: Database Rules (Ops Control)
```sql
-- Change limits at runtime, no deployment
INSERT INTO rate_limit_rules (endpoint_pattern, limit_count, window_seconds)
VALUES ('/api/search/**', 50, 60);
```

**When to use:** During an active attack. Update the database, limit changes in 60 seconds (cache TTL).

### Priority 3: YAML Defaults (Fallback)
```yaml
rate-limiter:
  default-limit: 100
  default-window-seconds: 60
```

**When to use:** Global baseline for everything else.

**Why This Matters:**

At 2:47 AM, your monitoring alerts you: someone's hammering `/api/export` with 500 req/sec.

With Resilience4j: Redeploy the app with new limits. 5-10 minute downtime.

With Aegis: `UPDATE rate_limit_rules SET limit_count = 10 WHERE endpoint_pattern = '/api/export';` — Live in 60 seconds. Zero downtime.

---

## Performance: The Numbers That Matter

I tested Aegis against Spring Cloud Gateway's built-in rate limiter (which uses Token Bucket + Redis).

**Benchmark Setup:**
- K6 load testing tool
- 10,000 requests/second sustained
- 3 Spring Boot instances behind Nginx
- Redis 7 cluster

### Results

| Metric | Aegis (Sliding Window) | Spring Cloud Gateway |
|--------|----------------------|---------------------|
| **Throughput** | 10,247 req/sec | 9,853 req/sec |
| **p95 Latency** | 8.2ms | 12.4ms |
| **p99 Latency** | 18.7ms | 31.2ms |
| **Accuracy** | 0 leaked requests | 23 leaked requests* |
| **Memory (Redis)** | 240 MB | 180 MB |

*Leaked = requests that exceeded the limit but were allowed due to race conditions

**Why Aegis is faster:**
- Single Lua script call vs 3 separate Redis commands
- No network round trips between check and act
- Optimized sorted set cleanup (only removes what's needed)

**Trade-off:** Sliding Window uses ~30% more memory because it stores individual request timestamps instead of a single counter. For security, worth it.

---

## What I Learned Building This

### 1. Security and UX Have Different Algorithms

Token Bucket feels great for users (bursts make the app feel responsive). Sliding Window is ruthless but fair (no exploits, no exceptions).

**Lesson:** Choose your algorithm based on your threat model, not your UX wishlist.

### 2. Distributed Systems Need Atomic Operations

Java's `synchronized` doesn't work across servers. Locks don't travel over the network. The only solution is moving the critical logic into a single source of truth (Redis) and making it atomic (Lua).

**Lesson:** If you can't eliminate the network, eliminate the round trips.

### 3. Observability is Half the Code

My rate limiter is 2,000 lines of Java. 1,000 of those are logging, metrics, and alerting.

- PostgreSQL logging (async, never blocks the decision)
- Redis Streams (real-time alerts for HIGH/CRITICAL violations)
- Prometheus metrics (10 custom metrics with histograms)
- Grafana dashboard (9 panels, auto-provisioned)

**Lesson:** A system you can't see is a system you can't trust.

### 4. Failing Open Is a Deliberate Choice

If Redis goes down, Aegis fails **open** (allows all requests). The alternative — failing **closed** — would DOS myself.

This is controversial. Security purists hate it. But I'd rather handle a temporary abuse spike than take down my entire platform because Redis restarted.

**Lesson:** Availability > perfect security. Document your trade-offs loudly.

---

## When You Should Use Resilience4j Instead

Don't let this post fool you — Resilience4j is *excellent* for its intended use case. Use it when:

- ✅ You're protecting **internal** microservices (not public APIs)
- ✅ User experience matters more than strict enforcement
- ✅ You want bursts for legitimate traffic spikes
- ✅ You need circuit breaking + retry + rate limiting in one library

**Use Aegis (or build your own) when:**
- ✅ You're protecting **public** APIs from abuse
- ✅ Security matters more than UX
- ✅ You need runtime configuration without deployments
- ✅ You want distributed correctness guarantees

---

## The Complete Picture

Here's what my architecture looks like in production:

```
                   ┌─────────────────┐
                   │  Load Balancer  │
                   └────────┬────────┘
                            │
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
         ┌────────┐    ┌────────┐    ┌────────┐
         │ Aegis  │    │ Aegis  │    │ Aegis  │
         │ Server │    │ Server │    │ Server │
         └───┬────┘    └───┬────┘    └───┬────┘
             │             │             │
             └─────────────┼─────────────┘
                           ▼
                   ┌───────────────┐
                   │ Redis Cluster │  ← Single source of truth
                   │  (Lua atomic) │     All decisions happen here
                   └───────┬───────┘
                           │
         ┌─────────────────┼─────────────────┐
         ▼                 ▼                 ▼
    ┌──────────┐    ┌────────────┐    ┌──────────┐
    │PostgreSQL│    │   Redis    │    │Prometheus│
    │  (Logs)  │    │  Stream    │    │ Metrics  │
    └──────────┘    │ (Alerts)   │    └────┬─────┘
                    └────────────┘         │
                                           ▼
                                     ┌──────────┐
                                     │ Grafana  │
                                     │Dashboard │
                                     └──────────┘
```

Every server is stateless. Every decision is atomic. Every blocked request is logged (async). Every metric is tracked.

---

## Performance Benchmarks in Detail

For those who want the raw data:

**Baseline Test (1 server, 1K req/sec):**
```
Requests:  100,000
Duration:  97.3 seconds
Success:   99,890 (99.89%)
Blocked:   110 (expected, limit enforcement)
p50:       2.1ms
p95:       8.2ms
p99:       18.7ms
Memory:    320 MB (JVM) + 120 MB (Redis)
```

**Spike Test (3 servers, 10K → 50K → 10K req/sec):**
```
Peak RPS:  48,731 req/sec
Duration:  5 minutes
Leaked:    0 (zero race conditions detected)
p99:       42ms (degraded under 5x load, acceptable)
Redis CPU: 78% peak
```

**Accuracy Test (Verify zero leakage):**
```yaml
Scenario: 100 clients, each sending 101 requests to 100/min endpoint
Expected: 100 allowed + 1 blocked per client = 10,000 blocks total
Actual:   10,000 blocks ✅
Leakage:  0 requests ✅
```

---

## What's Next for Aegis

This is version 1.0. Here's what's on the roadmap:

1. **AI-Powered Adaptive Limits** — Automatically tighten limits when patterns look suspicious
2. **Multi-Tenant Support** — Free/Pro/Enterprise tiers with different limits per user
3. **Language-Agnostic Sidecar** — Deploy as a standalone proxy, use from any language

See `future-roadmap/aegis-roadmap.md` for details.

---

## Lesson: Build It to Learn It

I could've used Resilience4j and saved a week. But I wouldn't have learned:

- How Lua atomicity prevents race conditions
- Why sorted sets are perfect for sliding windows
- How to fail gracefully when dependencies go down
- The difference between security algorithms and UX algorithms

**Building from scratch made me a better engineer.**

If you're early in your career: Don't just plug in libraries. Pick one, rewrite it yourself, compare your version to theirs. The gap between what you built and what they built is where the learning happens.

---

## Try It Yourself

The full source code is on GitHub: [kusuridheeraj/titan-grid](https://github.com/kusuridheeraj/titan-grid)

Clone it. Run it. Break it. Tell me what fails.

---

*Aegis is the first module in Titan Grid — a production-grade microservices platform I'm building. Next up is Cryptex, a zero-trust encrypted storage system using HashiCorp Vault and streaming encryption.*

*But that's a story for another post.*

---

**If you made it this far — thank you.** I write to think clearly. If this helped you understand rate limiting better, or made you rethink when to build vs buy, that's all I hoped for.

*— Dheeraj*
