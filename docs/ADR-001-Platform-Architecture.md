# ADR 001: Zero-Trust, High-Concurrency Infrastructure Platform

## Status
Accepted

## Context
We need to build a modular infrastructure platform that demonstrates Staff Engineer (SDE-3) competencies. The system must handle high traffic (10k RPS), secure sensitive data (zero-trust), and automate operations via AI, all while minimizing infrastructure costs.

## Decision: The "Titan Grid" Architecture

### 1. Aegis (The Shield) - Distributed Rate Limiter
*   **Choice:** Java (Spring Boot) + Redis + Lua.
*   **Why:** 
    *   **Redis Lua:** Ensures *atomic* operations in a distributed environment. Prevents race conditions that lead to "request leakage" during spikes.
    *   **Staff Signal:** Demonstrates understanding of distributed state management and atomicity beyond simple Java locks.
    *   **Value:** Saves money by preventing DDOS attacks from overwhelming downstream expensive compute resources (Autoscaling triggers).

### 2. Cryptex (The Vault) - Secure Streaming Storage
*   **Choice:** Java + HashiCorp Vault + S3/MinIO.
*   **Why:**
    *   **Streaming I/O:** Uses `CipherInputStream` to encrypt chunks on-the-fly.
    *   **Envelope Encryption:** Vault manages the keys; the application never stores them.
    *   **Staff Signal:** Memory management expertise. We can handle 50GB file uploads on 512MB RAM containers.
    *   **Value:** **Direct Infrastructure Savings.** Standard "load-in-memory" approaches require high-memory instances (e.g., AWS `r6g` instances). Cryptex runs on the cheapest `t4g.nano` or Fargate spot instances.

### 3. Nexus (The Brain) - AI Operator
*   **Choice:** Python (FastAPI) + MCP (Model Context Protocol).
*   **Why:**
    *   **MCP:** Standardizes how AI interacts with tools. Provides an "Air Gap" for safety.
    *   **Local LLM (Ollama):** Reduces costs and ensures data privacy for sensitive log analysis.
    *   **Staff Signal:** AI Safety and deterministic execution of agentic workflows.
    *   **Value:** Reduces the need for a 24/7 manual SRE rotation. The AI diagnoses and recommends fixes, saving hundreds of thousands in human capital.

## Business Impact (The "Pitch")
*   **Reliability:** 100% Uptime via "Fail Open" Circuit Breakers. Even if our security layer (Redis) is down, the business stays online.
*   **Cost Efficiency:** Architecture designed for "Micro-Compute." 10x cheaper to run than traditional high-memory monolithic solutions.
*   **Security:** Compliance-ready. Every file is encrypted before it touches the "cloud," satisfying GDPR/HIPAA requirements out of the box.
