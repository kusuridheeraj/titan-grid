# ============================================
# Aegis Rate Limiter - Multi-Stage Dockerfile
# ============================================
# Stage 1: Build with Maven
# Stage 2: Minimal JRE runtime
# ============================================

# ---- Stage 1: Build ----
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Cache Maven dependencies (separate layer for faster rebuilds)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application (skip tests - run in CI/CD pipeline)
RUN mvn clean package -DskipTests -B

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:17-jre-alpine

# Security: Run as non-root user
RUN addgroup -S aegis && adduser -S aegis -G aegis

WORKDIR /app

# Copy only the built JAR from builder stage
COPY --from=builder /app/target/aegis-*.jar app.jar

# Environment variables (with sensible defaults)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication" \
    REDIS_HOST=redis \
    REDIS_PORT=6379 \
    POSTGRES_HOST=postgres \
    POSTGRES_PORT=5432 \
    POSTGRES_DB=titan_grid \
    SERVER_PORT=8080

# Health check
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Expose application port
EXPOSE 8080

# Switch to non-root user
USER aegis

# Start application with JVM tuning
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
