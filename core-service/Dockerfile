FROM eclipse-temurin:25-jdk-jammy AS builder
WORKDIR /workspace

COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon :core-service:bootJar -x test

FROM eclipse-temurin:25-jre-jammy AS runtime
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r spring \
    && useradd -r -g spring spring

COPY --from=builder /workspace/core-service/build/libs/*.jar /app/app.jar

EXPOSE 8080 8081
USER spring:spring

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
