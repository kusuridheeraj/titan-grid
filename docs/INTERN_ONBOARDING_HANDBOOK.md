# 🛡️ The Titan Grid / Aegis Intern Handbook
**From High School to Staff Engineer: The Complete Journey**

**Welcome to the Team.**  
This is not just documentation. This is your mentorship in document form. By the end of this handbook, you will understand how to think like a Staff Engineer building distributed systems.

---

## 📖 Part 1: The "Why" - What Problem Are We Actually Solving?

### Real-World Scenario: The Concert Ticket Catastrophe
Imagine you're at Ticketmaster on the day Taylor Swift announces a surprise show. Within milliseconds:
- 500,000 people refresh the page
- Each person's browser sends 10 requests per second (checking seat availability)
- That's **5 MILLION requests per second** hitting your servers

**Without Aegis:** Your database crashes in 3 seconds. Nobody gets tickets. Twitter explodes with complaints.  
**With Aegis:** We let through 10,000 requests/second. The 4,990,000 excess requests get HTTP 429 ("Too Many Requests"). The system stays alive. People actually get tickets.

### The Three Pain Points We Solve

#### 1. **DDoS Protection** (Distributed Denial of Service)
- **Attack Scenario:** A hacker uses 10,000 infected computers to flood your login endpoint.
- **Without Aegis:** Your login server dies. Legitimate users can't access their accounts.
- **With Aegis:** We detect the pattern (single IP sending 1000 req/sec) and block it. Legit users continue working.

#### 2. **Cost Control** (The $100,000 AWS Bill)
- **Bug Scenario:** A junior developer deploys code with an infinite loop: `while(true) { callAPI(); }`
- **Without Aegis:** AWS charges $0.0001 per request. 1 billion requests = $100,000 overnight.
- **With Aegis:** We cap the user at 100 req/min. Maximum damage: $14.40.

#### 3. **Fairness** (The Noisy Neighbor Problem)
- **Scenario:** Company A pays $10/month. Company B pays $10,000/month. They share your server.
- **Without Aegis:** Company A can use 99% of CPU, starving Company B.
- **With Aegis:** Company A gets 100 req/min. Company B gets 10,000 req/min. Fair is fair.

---

## 🏗️ Part 2: Architecture Deep Dive (Every Decision Explained)

### Decision #1: Why Redis? (vs In-Memory HashMap)

**The Naive Approach:**
```java
// DON'T DO THIS
Map<String, Integer> counts = new HashMap<>();
if (counts.get(userId) < 100) {
    counts.put(userId, counts.get(userId) + 1);
}
```

**Why This Fails in Production:**
1. **Scaling:** You deploy to 3 servers. Each has its own HashMap. User gets 3x the limit (300 instead of 100).
2. **Crashes:** Server restarts? All counts reset to zero. The attacker just waits 2 seconds and attacks again.
3. **Memory:** 1 million users × 1KB data = 1GB RAM per server. Expensive.

**The Senior Solution: Redis**

```java
// Correct approach
Long count = redisTemplate.opsForValue().increment("user:123:count");
if (count <= 100) { /* allow */ }
```

**Why Redis Wins:**
- **Centralized:** All 3 servers check the same Redis. No duplication.
- **Persistent:** Redis dumps data to disk every 60 seconds. Survives crashes.
- **Fast:** 100,000 operations/second on a laptop. Millions/second on production hardware.

**Trade-off Accepted:** Redis is a "single point of failure." If Redis dies, rate limiting stops. **Mitigation:** We run Redis in cluster mode (3 nodes). 2 can die, and it still works.

---

### Decision #2: Why Lua Scripts? (Atomicity & Race Conditions)

**The Race Condition Nightmare:**
```java
// Thread A and Thread B execute this simultaneously
long current = redis.get("count");  // Both see "99"
if (current < 100) {
    redis.incr("count");  // Both increment. Count becomes 101.
}
```

**Why This is Catastrophic:**
- Limit is 100.
- Both threads think they'

re #100.
- Actual count: 102.
- **Your server just processed 2 extra requests during a DDoS attack.**

**The Solution: Lua Scripts**
```lua
-- This executes ATOMICALLY in Redis
local current = redis.call('GET', key)
if tonumber(current) < limit then
    return redis.call('INCR', key)
else
    return 0
end
```

**Why This Works:**
- Redis is **single-threaded**. Only ONE command executes at a time.
- The Lua script is treated as ONE command.
- Mathematical guarantee: No race conditions.

**Trade-off:** Lua is harder to debug than Java. But correctness > convenience.

---

### Decision #3: Why Sliding Window? (vs Fixed Window)

**Fixed Window (The Easy Way):**
```
12:00:00 - 12:00:59 → Allow 100 requests
12:01:00 - 12:01:59 → Reset, allow 100 more
```

**The Fatal Flaw:**
```
User sends:
- 100 requests at 12:00:59
- 100 requests at 12:01:00
Total: 200 requests in 1 second.
Your server: 💥
```

**Sliding Window (The Hard Way):**
```
At 12:01:30, we look at "requests from 12:00:30 to 12:01:30"
If count > 100, block.
```

**Why This is Better:**
- **Smooth Traffic:** No sudden bursts.
- **Accurate:** 100 req/min means EXACTLY 100 in any 60-second window.

**Trade-off:** More memory (we store timestamps for each request). But 1000 timestamps = 8KB. Acceptable.

---

### Decision #4: Hybrid Configuration (Annotation > DB > YAML)

**The Problem:**
- **Hardcoded = Inflexible:** If login limit is in code, you need to redeploy to change it.
- **Database-only = Slow:** Querying PostgreSQL for EVERY request adds 5-10ms latency.

**The Solution: 3-Tier Priority System**

```java
// Priority 1: Annotation (Developer says "this is CRITICAL")
@RateLimit(limit = 5, windowSeconds = 60)
public String login() { ... }

// Priority 2: Database (Ops says "we're under attack, tighten ALL limits")
INSERT INTO rules (endpoint, limit) VALUES ('/api/login', 3);

// Priority 3: YAML (safe default)
rate-limiter:
  default-limit: 100
```

**How We Cache:**
- First request: Query database (10ms).
- Store result in-memory for 60 seconds.
- Next 5,999 requests: Read from cache (0.001ms).

**Trade-off:** Cache can be stale for up to 60 seconds. If you update the DB, it takes 60 seconds to apply. **Acceptable** for non-critical changes.

---

## 🛠️ Part 3: Building & Running (Production Standards)

### Environment Setup (Strict Requirements)

**Software Versions:**
- **Java:** 17.0.8+ (LTS version)
- **Maven:** 3.8+
- **Docker Desktop:** Latest
- **Git:** 2.40+

**Why these specific versions?**
- Java 17 is the latest Long-Term Support (LTS). Java 21 is too new (libraries aren't ready).
- Maven 3.8+ has security patches. 3.6 has a critical vulnerability (CVE-2021-26291).

---

### Step-by-Step: Running on Windows

#### Step 1: Clone the Repository
```powershell
cd C:\PlayStation\assets
git clone https://github.com/kusuridheeraj/titan-grid.git
cd titan-grid
```

#### Step 2: Configure Environment
```powershell
# Copy template
cp infra\.env.template infra\.env

# Edit with Notepad or VS Code
notepad infra\.env
```

**Critical Settings:**
```env
POSTGRES_PASSWORD=your-secure-password-here
REDIS_PASSWORD=another-secure-password
```

#### Step 3: Start Infrastructure
```powershell
# Start Redis, PostgreSQL, Prometheus, Grafana
docker-compose -f infra/docker-compose.yml up -d

# Verify everything is running
docker ps
```

**Expected Output:**
```
NAME                  STATUS
redis                 Up 5 seconds
postgres              Up 5 seconds
prometheus            Up 5 seconds
grafana               Up 5 seconds
```

#### Step 4: Build the Application
```powershell
cd aegis

# Clean old artifacts
mvn clean

# Compile code
mvn compile

# Run tests (CRITICAL - never skip this)
mvn test

# Package JAR
mvn package
```

**Build Artifact Location:** `target/aegis-1.0.0-SNAPSHOT.jar`

#### Step 5: Run the Application
```powershell
# Option A: Via Maven (easier for development)
mvn spring-boot:run

# Option B: Via JAR (closer to production)
java -jar target/aegis-1.0.0-SNAPSHOT.jar
```

**Verify It's Running:**
```powershell
# Health check
curl http://localhost:8080/actuator/health

# Expected: {"status":"UP"}
```

---

### Step-by-Step: Building Docker Image

```powershell
# Build multi-stage image
docker build -t titan-grid/aegis:1.0.0 -f aegis/Dockerfile .

# Run the container
docker run -p 8080:8080 --env-file infra/.env titan-grid/aegis:1.0.0
```

**Why Multi-Stage Build?**
```dockerfile
# Stage 1: Build (uses Maven + JDK)
FROM maven:3.9-eclipse-temurin-17 AS builder
RUN mvn package

# Stage 2: Runtime (uses only JRE - 70% smaller!)
FROM eclipse-temurin:17-jre-alpine
COPY --from=builder /app/target/*.jar app.jar
```

**Result:** Build image = 800MB. Runtime image = 180MB. Faster deployments, lower costs.

---

## 🧪 Part 4: Testing (No Excuses, No Shortcuts)

###  1. Unit Tests - Fast Feedback

**Run:**
```powershell
mvn test
```

**What This Tests:**
- `RateLimitRuleResolverTest`: Priority system (Annotation beats DB beats YAML?)
- `SlidingWindowAlgorithmTest`: Math correctness (100 req/min enforced?)
- `ClientIdExtractorTest`: Can we identify users correctly?

**Coverage Requirement:** Minimum 80%. Run `mvn jacoco:report` to check.

---

### 2. Integration Tests - Real Dependencies

**Using Testcontainers:**
```java
@Testcontainers
class RateLimiterIntegrationTest {
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
        
    @Test
    void shouldBlockAfter100Requests() {
        // Spins up REAL Redis container
        // Runs test
        // Destroys container
    }
}
```

**Why This Matters:**
- Unit tests mock Redis. Mocks can lie.
- Integration tests use **real Redis**. If this passes, production will work.

---

### 3. Load Testing - K6 Scripts

**Install K6:**
```powershell
winget install k6
```

**Run Baseline Test:**
```powershell
k6 run aegis/k6/k6-baseline.js
```

**Expected Results:**
```
✓ http_req_duration.............avg=15ms  max=50ms
✓ http_req_failed...............0.00%
✓ http_reqs.....................10,000 req/sec
```

**If ANY of these fail, DO NOT deploy to production.**

---

## 🔄 Part 5: CI/CD Pipeline (GitHub Actions)

### Why Automate?
- **Humans forget:** You might skip tests before pushing.
- **Humans are inconsistent:** Different developers use different Java versions.
- **Automation doesn't sleep:** Tests run at 3 AM if someone pushes code.

### GitHub Actions Configuration

Create `.github/workflows/ci.yml`:

```yaml
name: Aegis CI Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
          
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    
    steps:
    - name: Checkout Code
      uses: actions/checkout@v4
      
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: 'maven'
    
    - name: Run Unit Tests
      run: mvn -B test --file aegis/pom.xml
      
    - name: Run Integration Tests
      run: mvn -B verify --file aegis/pom.xml
      env:
        SPRING_REDIS_HOST: localhost
        SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/aegis
        SPRING_DATASOURCE_PASSWORD: test
    
    - name: Build JAR
      run: mvn -B package -DskipTests --file aegis/pom.xml
      
    - name: Upload Artifact
      uses: actions/upload-artifact@v4
      with:
        name: aegis-jar
        path: aegis/target/*.jar
        retention-days: 30
    
  security-scan:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    
    - name: Run Trivy Security Scan
      uses: aquasecurity/trivy-action@master
      with:
        scan-type: 'fs'
        scan-ref: 'aegis/'
        severity: 'CRITICAL,HIGH'
```

**What This Does:**
1. **Trigger:** Runs on every push to `main` or any Pull Request.
2. **Services:** Starts Redis + PostgreSQL in Docker.
3. **Tests:** Runs unit + integration tests with REAL databases.
4. **Security:** Scans for vulnerabilities (e.g., Log4Shell).
5. **Artifact:** Saves the JAR file for 30 days.

**Branch Protection Rule:**
Go to GitHub → Settings → Branches → Add Rule:
- ✅ Require status checks to pass
- ✅ Require branches to be up to date
- ✅ Require conversation resolution before merging

**Result:** Impossible to merge broken code.

---

## 📦 Part 6: Publishing as a Library (Maven Central)

To make Aegis usable by others (like how they use `spring-boot-starter-web`):

### Step 1: Create a Spring Boot Starter Module

```
aegis-spring-boot-starter/
├── pom.xml
├── src/main/java/
│   └── com/titan/aegis/autoconfigure/
│       ├── AegisAutoConfiguration.java
│       └── AegisProperties.java
└── src/main/resources/
    └── META-INF/spring.factories
```

**AegisAutoConfiguration.java:**
```java
@Configuration
@ConditionalOnProperty(prefix = "aegis", name = "enabled", havingValue = "true")
public class AegisAutoConfiguration {
    
    @Bean
    public AegisFilter aegisFilter(RateLimiterService rateLimiterService) {
        return new AegisFilter(rateLimiterService);
    }
    
    @Bean
    public RateLimiterService rateLimiterService(RedisTemplate<String, String> redisTemplate) {
        return new RateLimiterService(redisTemplate);
    }
}
```

**spring.factories:**
```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.titan.aegis.autoconfigure.AegisAutoConfiguration
```

### Step 2: Configure for Maven Central

**pom.xml additions:**
```xml
<groupId>io.github.kusuridheeraj</groupId>
<artifactId>aegis-spring-boot-starter</artifactId>
<version>1.0.0</version>

<licenses>
    <license>
        <name>Apache License 2.0</name>
        <url>https://www.apache.org/licenses/LICENSE-2.0</url>
    </license>
</licenses>

<developers>
    <developer>
        <name>Dheeraj Kusuri</name>
        <email>kusuri.dheeraj2014@gmail.com</email>
    </developer>
</developers>

<scm>
    <url>https://github.com/kusuridheeraj/titan-grid</url>
</scm>

<distributionManagement>
    <repository>
        <id>ossrh</id>
        <url>https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/</url>
    </repository>
</distributionManagement>
```

### Step 3: Sign & Deploy
```powershell
# Generate GPG key (one-time setup)
gpg --gen-key

# Deploy to staging
mvn clean deploy -P release

# Release to Maven Central
mvn nexus-staging:release
```

### Step 4: How Others Use It
```xml
<dependency>
    <groupId>io.github.kusuridheeraj</groupId>
    <artifactId>aegis-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

Their `application.yml`:
```yaml
aegis:
  enabled: true
  default-limit: 100
  default-window-seconds: 60
```

**That's it. Zero code needed. Auto-configured.**

---

## ⚠️ Part 7: Safety Checklist (For Interns & Seniors Alike)

### Never Commit These
- ❌ `.env` files
- ❌ API keys (AWS, Azure, Stripe)
- ❌ Database passwords
- ❌ SSL certificates

**Use `.gitignore`:**
```
*.env
*.key
*.pem
secrets/
```

### Database Migrations
**WRONG:**
```sql
-- Don't manually run this in production!
ALTER TABLE users ADD COLUMN email VARCHAR(255);
```

**RIGHT:**
Use versioned migration scripts:
```
infra/db-migrations/
├── V1__create_tables.sql
├── V2__add_email_column.sql
└── V3__add_index_on_email.sql
```

Run with Flyway/Liquibase:
```powershell
mvn flyway:migrate
```

### The N+1 Query Problem
**BAD:**
```java
List<User> users = userRepository.findAll();  // 1 query
for (User user : users) {
    Profile profile = profileRepository.findByUserId(user.getId());  // N queries
}
// Total: 1 + N queries (if N=1000, that's 1001 queries!)
```

**GOOD:**
```java
List<User> users = userRepository.findAllWithProfiles();  // 1 query with JOIN
// Total: 1 query
```

**How to Detect:** Enable Hibernate logging:
```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
```

If you see 1000+ SELECT statements, you have N+1.

---

## 💻 Part 8: Platform Differences (Windows vs Linux vs macOS)

You're developing on **Windows**. Production runs on **Linux**. Here's what changes:

### File Paths
| OS | Path Separator | Example |
|----|----------------|---------|
| Windows | Backslash `\` | `C:\Users\kusur\file.txt` |
| Linux/macOS | Forward Slash `/` | `/home/kusur/file.txt` |

**Java Solution:**
```java
// WRONG (breaks on Linux)
String path = "C:\\Users\\kusur\\file.txt";

// RIGHT (works everywhere)
Path path = Path.of("Users", "kusur", "file.txt");
```

### Line Endings
| OS | Line Ending | Hex |
|----|-------------|-----|
| Windows | CRLF | `\r\n` (0D 0A) |
| Linux/macOS | LF | `\n` (0A) |

**Problem:** You create a `.sh` script on Windows:
```bash
#!/bin/bash
echo "Hello"
```

On Linux:
```
bash: ./script.sh: /bin/bash^M: bad interpreter
```

**Solution:** Configure Git:
```powershell
git config --global core.autocrlf true
```

### Shell Scripts
| OS | Shell | Extension |
|----|-------|-----------|
| Windows | PowerShell | `.ps1` |
| Linux | Bash | `.sh` |
| macOS | Zsh (or Bash) | `.sh` |

**Best Practice:** Use a `Makefile` that works everywhere:
```makefile
.PHONY: build test run

build:
	mvn clean package

test:
	mvn test

run:
	java -jar target/aegis-1.0.0-SNAPSHOT.jar
```

Run with:
```powershell
make build  # Works on Windows (if you install Make)
make build  # Works on Linux
make build  # Works on macOS
```

### Docker on Windows
Windows uses **WSL2** (Windows Subsystem for Linux) as a backend for Docker.

**Verify:**
```powershell
wsl -l -v
```

**Expected:**
```
  NAME            STATE           VERSION
* Ubuntu-22.04    Running         2
  docker-desktop  Running         2
```

If VERSION shows 1, upgrade:
```powershell
wsl --set-version Ubuntu-22.04 2
```

---

## 🎯 Part 9: How to Contribute (Open Source Mindset)

### Contribution Workflow

1. **Fork the Repository**
   ```bash
   # On GitHub, click "Fork"
   git clone https://github.com/YOUR-USERNAME/titan-grid.git
   ```

2. **Create a Feature Branch**
   ```bash
   git checkout -b feature/add-ip-whitelist
   ```

3. **Write Tests First (TDD)**
   ```java
   @Test
   void shouldAllowWhitelistedIP() {
       // Arrange
       String whitelistedIP = "192.168.1.1";
       
       // Act
       RateLimitDecision decision = limiter.checkIP(whitelistedIP);
       
       // Assert
       assertTrue(decision.isAllowed());
   }
   ```

4. **Implement the Feature**
   ```java
   public RateLimitDecision checkIP(String ip) {
       if (whitelistedIPs.contains(ip)) {
           return RateLimitDecision.allowed();
       }
       return checkRedis(ip);
   }
   ```

5. **Run All Tests**
   ```bash
   mvn verify
   ```

6. **Commit with Conventional Commits**
   ```bash
   git commit -m "feat(security): add IP whitelist functionality
   
   - Allow admins to whitelist IPs via database
   - Bypass rate limiting for whitelisted IPs
   - Add integration tests for whitelist feature
   
   Closes #42"
   ```

7. **Push and Create Pull Request**
   ```bash
   git push origin feature/add-ip-whitelist
   ```

### Code Review Checklist
Before submitting PR, verify:
- ✅ All tests pass (`mvn verify`)
- ✅ Code coverage ≥ 80% (`mvn jacoco:report`)
- ✅ No security vulnerabilities (`mvn dependency-check:check`)
- ✅ Javadoc for public methods
- ✅ Updated CHANGELOG.md

---

## 📚 Part 10: Learning Path (Your Next 6 Months)

### Month 1: Master the Basics
- Read all code in `aegis/src/main/java/com/titan/aegis/`
- Understand every line of `sliding_window.lua`
- Run all tests locally
- Break something and fix it

### Month 2: Contribute
- Pick an issue tagged `good-first-issue`
- Implement + Test + Document
- Submit your first Pull Request

### Month 3: Observability
- Add a new Prometheus metric
- Create a Grafana dashboard panel
- Implement a custom alert rule

### Month 4: Performance
- Run K6 load tests
- Identify bottleneck (hint: check database queries)
- Optimize and re-test

### Month 5: Production Operations
- Simulate Redis failure (kill the container)
- Watch how the system behaves
- Implement circuit breaker pattern

### Month 6: Architecture
- Design a new feature (e.g., "Geographic Rate Limiting")
- Write ADR (Architecture Decision Record)
- Present to the team

---

## 🚀 Final Words

You're not just writing code. You're **protecting millions of users** from service outages. You're **saving companies thousands of dollars** in AWS bills. You're **learning skills** that will make you a Staff Engineer.

**Three Rules:**
1. **Test everything.** Bugs in production cost money.
2. **Document everything.** Your future self (and team) will thank you.
3. **Ask questions.** There are no stupid questions. Only unasked ones.

Now go build something amazing. 🛡️

---

**Emergency Contacts:**
- Slack: `#aegis-dev`
- Email: kusuri.dheeraj2014@gmail.com
- Docs: `docs/ARCHITECTURE_DECISIONS.md`
