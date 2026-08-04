# syntax=docker/dockerfile:1.7

# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
LABEL stage=builder
WORKDIR /app

# Copy maven files
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy source code
COPY src ./src

# Build application
RUN mvn -B -DskipTests clean package

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

# Labels
LABEL maintainer="Banking API Team"
LABEL version="1.0"
LABEL description="Enterprise Banking API"

# Install curl for healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN useradd -r -u 1000 -d /home/appuser -s /sbin/nologin -c "Docker app user" appuser

WORKDIR /app

# Copy JAR from builder
COPY --from=builder --chown=appuser:appuser /app/target/banking-api-*.jar app.jar

# Set environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SPRING_PROFILES_ACTIVE=docker

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/health || exit 1

# Entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]
