# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy pom files
COPY ../pom.xml ./
COPY pom.xml ./

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
#RUN addgroup -S appuser && adduser -S appuser -G appuser

# Copy JAR from builder stage
COPY --from=builder /build/target/user-service-1.0.0.jar app.jar

# Change ownership to appuser
#RUN chown -R appuser:appuser /app

# Switch to non-root user
#USER appuser

# Expose port (service-specific)
EXPOSE 8001

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8001/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]