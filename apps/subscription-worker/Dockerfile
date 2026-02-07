# Multi-stage build for subscription-worker
# Optimized for cloud deployment (GCP, AWS, Azure)

# Build stage
FROM gradle:8.5-jdk17 AS builder

WORKDIR /workspace

# Copy Gradle configuration
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Copy all modules (required for multi-module build)
COPY modules ./modules

# Copy Worker application
COPY apps/subscription-worker ./apps/subscription-worker

# Build the Worker (skip tests for faster builds)
RUN ./gradlew :apps:subscription-worker:build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Add non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy built JAR from builder stage
COPY --from=builder /workspace/apps/subscription-worker/build/libs/*.jar app.jar

# Change ownership to non-root user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring

# No port exposure needed (background worker)

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Run the worker
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
