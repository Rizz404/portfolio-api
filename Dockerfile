# syntax=docker/dockerfile:1

# ---------- Build stage ----------
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Cache dependencies separately from source so code changes don't bust the layer
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && \
    ./mvnw dependency:go-offline -B

COPY src/ src/
RUN ./mvnw clean package -DskipTests -B && \
    java -Djarmode=tools -jar target/*.jar extract --layers --destination target/extracted

# ---------- Runtime stage ----------
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

RUN apk add --no-cache curl tzdata && \
    addgroup -S spring && adduser -S spring -G spring && \
    mkdir -p /app/logs && chown -R spring:spring /app/logs

# Layered copy: dependencies change least often, application layer changes most
COPY --from=build --chown=spring:spring /app/target/extracted/dependencies/ ./
COPY --from=build --chown=spring:spring /app/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=spring:spring /app/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /app/target/extracted/application/ ./

USER spring:spring

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
