# 🎓 Intern's Guide to Project Aegis

**Welcome to the team!**  
This document is your bible. It explains not just *how* to run the code, but *why* we built it this way. Whether you're a high school intern or a future Staff Engineer, mastering these concepts is non-negotiable before you touch the codebase.

---

## 🧐 Part 1: The "Why" – Understanding the Mission

### What are we solving?
Imagine a popular ticket booking site (like BookMyShow or Ticketmaster) when a superstar's concert goes on sale. Millions of people hit the "Buy" button at the exact same second.
- **Without Aegis:** The database crashes, the server melts, and nobody gets tickets.
- **With Aegis:** We act as the bouncer. We only let 10,000 people in per second. The 10,001st person gets told "Please wait" (HTTP 429). The system stays alive.

### What pain does this solve?
1. **DDoS Protection:** Prevents attackers from flooding us with junk traffic.
2. **Fairness:** Prevents one user (or bot) from hogging all the resources.
3. **Cost Control:** Prevents our cloud bill from exploding due to unexpected traffic spikes.
4. **Reliability:** Ensures the service is always up for legitimate users.

---

## 🏗️ Part 2: Architecture & Design Philosophy

We didn't just "hack this together." Every decision was a trade-off. Here is the thought process you must understand:

### 1. Why Sliding Window? (Vs Token Bucket)
- **Token Bucket (The "Easy" Way):** Allows bursts. If you have 10 tokens, you can spend them all in 1 millisecond.
- **Sliding Window (Our Way):** Strict enforcement. If the limit is 100 req/min, you CANNOT do 100 requests in 1 second.
- **Why we chose Sliding Window:** We are building a **security-first** rate limiter. Bursts are attack vectors. We want smooth, predictable traffic, not jagged spikes.

### 2. Why Redis + Lua Scripts?
- **The Problem:** "Read-Modify-Write" race condition.
  - Thread A reads count = 9.
  - Thread B reads count = 9.
  - Thread A increments to 10.
  - Thread B increments to 10.
  - **Result:** Both think they are within the limit of 10. We let 11 users in! ❌
- **The Solution:** Lua scripts run **atomically** inside Redis. It's like a database transaction. No other command can run while our script executes. Reliability > Speed.

### 3. Why Hybrid Configuration?
- **The Problem:** Hardcoding limits in code means redeploying to change them. Keeping them *only* in the DB means higher latency.
- **The Solution:** A 3-Tier Priority System:
  1. **Annotation (`@RateLimit`)**: Developers know their API best.
  2. **Database (Runtime)**: Ops team can tighten limits during an attack without restarting.
  3. **YAML (Config)**: Safe defaults for everything else.

---

## 🛠️ Part 3: How to Build, Run & Test (The Hard Way)

We don't do "it works on my machine." We build production artifacts.

### 1. Building the JAR (Java Artifact)
We use Maven. You must build from the root directory.

```bash
# Clean previous builds and package a new JAR
./mvnw clean package -DskipTests
```

*Artifact Location:* `target/aegis-0.0.1-SNAPSHOT.jar`

### 2. Building the Docker Image
We use a **Multi-Stage Build** to keep images small (Alpine Linux).

```bash
docker build -t titan-grid/aegis:latest .
```

### 3. Running the Full Stack (Infrastructure)
You need Redis (cache) and PostgreSQL (logs). Do not install them locally. Use Docker Compose.

```bash
# Start everything in the background
docker-compose up -d
```

### 4. Running the Load Tests (K6)
We use **K6** to simulate 10,000 users punching our API.

```bash
# Install K6 (Windows)
winget install k6

# Run the spike test
k6 run k6/k6-spike.js
```

**Pass Criteria:**
- **99%** of requests under 50ms latency.
- **0%** error rate (except 429s).
- **10,000** req/sec throughput.

---

## 🚧 Part 4: Contribution Guidelines (How Not To Mess Up)

### Rule #1: Do No Harm
- Never commit code that breaks the build.
- Never commit secrets (API keys, passwords) to Git.

### Rule #2: Test Driven Development (TDD)
Before you write a new feature, write a test that fails.
- Adding a new limiter algo? Write a test case in `RateLimiterServiceTest.java` first.

### Rule #3: The "Intern Guardrails"
To prevent you from accidentally destroying the project:
1. **Pre-Commit Hooks:** We use checkstyle. If your code looks messy, Git won't let you commit.
2. **Branch Protection:** You cannot push to `main`. You must create a Pull Request (PR).
3. **CI/CD Pipeline:** GitHub Actions will automatically run all tests when you push. If *one* test fails, your PR is rejected.

---

## 🌍 Part 5: Cross-Platform Compatibility

You are developing on **Windows**. But servers run **Linux**.

| Feature | Windows (Dev) | Linux (Prod) | macOS (Dev) |
|---------|--------------|--------------|-------------|
| **Line Endings** | CRLF (`\r\n`) | LF (`\n`) | LF (`\n`) |
| **Paths** | Backslash `\` | Forward Slash `/` | Forward Slash `/` |
| **Scripts** | `.bat` or PowerShell | `.sh` (Bash) | `.sh` (Bash) |
| **Docker** | Uses WSL2 backend | Native | Native (Unix) |

**Intern Note:** Always use `Path.of("folder", "file")` in Java. Never hardcode `"folder\file"` strings, or your code will crash on the Linux server.

---

## 📦 Part 6: How Others Use Our Project

We want other companies to use Aegis as a library.

### 1. As a Maven Dependency
They add this to their `pom.xml`:

```xml
<dependency>
    <groupId>com.titan</groupId>
    <artifactId>aegis-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. As a Sidecar (Docker)
They run our container next to their app:

```yaml
services:
  my-app:
    image: my-company/app
  aegis:
    image: titan-grid/aegis:latest
    network_mode: service:my-app
```

---

*Now, go forth and code. But confirm your architecture with the Staff Engineer (me) before writing a single line.* 🚀
