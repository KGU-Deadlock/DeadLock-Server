FROM eclipse-temurin:25-jre-noble AS runtime
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r spring \
    && useradd -r -g spring spring

# 사전에 `./gradlew :topic:bootJar` 로 빌드된 jar를 복사한다.
COPY topic/build/libs/topic-service.jar /app/app.jar

EXPOSE 8082 8092
USER spring:spring

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
