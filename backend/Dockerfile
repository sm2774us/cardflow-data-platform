# syntax=docker/dockerfile:1.7
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package \
 && java -Djarmode=layertools -jar target/*.jar extract --destination /layers

FROM eclipse-temurin:17-jre-alpine
# Non-root, arbitrary-UID friendly (OpenShift restricted SCC): group 0 owns writable paths.
RUN addgroup -g 10001 -S app && adduser -u 10001 -S app -G app && mkdir -p /app /data && chown -R 10001:0 /app /data && chmod -R g=u /app /data
WORKDIR /app
COPY --from=build --chown=10001:0 /layers/dependencies/ ./
COPY --from=build --chown=10001:0 /layers/spring-boot-loader/ ./
COPY --from=build --chown=10001:0 /layers/snapshot-dependencies/ ./
COPY --from=build --chown=10001:0 /layers/application/ ./
USER 10001
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" LAKE_LOCAL_DIR=/data/lake
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
