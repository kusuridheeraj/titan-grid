FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S cryptex && adduser -S cryptex -G cryptex
USER cryptex:cryptex
COPY --from=builder /app/target/cryptex-*.jar app.jar
EXPOSE 8081
CMD ["java", "-jar", "app.jar"]
