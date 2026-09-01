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
# * Default 8080, tapi bisa di-override lewat env APP_PORT di runtime (docker-compose.yml) --
# * dipakai bareng buat EXPOSE, HEALTHCHECK, dan port yang didengerin Spring (lihat ENTRYPOINT).
ENV APP_PORT=8080
EXPOSE ${APP_PORT}

# * Cek grup "core" (db + diskSpace + ping), bukan root /actuator/health -- root tetap ikut
# * redis (buat visibility manual), tapi grup "core" sengaja exclude redis biar container ini
# * gak ke-flag unhealthy/restart cuma gara-gara redis down (lihat application.properties).
# * Shell form (bukan JSON array) biar $APP_PORT di-resolve pas healthcheck jalan, bukan pas build.
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD curl -f http://localhost:${APP_PORT}/api/v1/actuator/health/core || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=$APP_PORT -jar app.jar"]
