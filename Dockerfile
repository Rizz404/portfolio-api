# ---- Stage build ----
FROM docker.io/eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# * Selalu inget kalo argument paling akhir pada COPY itu destinationnya
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw && \
    ./mvnw dependency:go-offline -B

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw clean package -DskipTests -B

# ---- Stage serve ----
FROM docker.io/eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

# * Backslash itu biar gak numpuk di satu line, jadi lebih clean
# * Buat jalanin command disatu instruksi pakai &&, biar kalo 1 gagal gak terus nyelonong
RUN apk add --no-cache curl tzdata && \
    addgroup -S spring && adduser -S spring -G spring && \
    mkdir -p /app/logs && chown -R spring:spring /app/logs

COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

USER spring:spring

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
