# Multi-stage build for subscription-api
# Optimized for cloud deployment (GCP, AWS, Azure)

# Build stage
FROM gradle:8.5-jdk17 AS builder

WORKDIR /workspace

# Copy Gradle configuration
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Copy all modules (required for multi-module build)
COPY modules ./modules

# Copy API application
COPY apps/subscription-api ./apps/subscription-api

# Build the API (skip tests for faster builds)
RUN ./gradlew :apps:subscription-api:build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Add non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy built JAR from builder stage
COPY --from=builder /workspace/apps/subscription-api/build/libs/*.jar app.jar

# Change ownership to non-root user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
