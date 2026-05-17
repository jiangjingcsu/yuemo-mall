FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="yuemo"
LABEL description="月魔商城 — Spring Boot 3.2 单体应用 + JDK 17"

RUN addgroup -S yuemo && adduser -S yuemo -G yuemo

WORKDIR /app

ARG JAR_FILE=yuemo-server/target/yuemo-server-*.jar
COPY ${JAR_FILE} /app/yuemo-mall.jar

RUN chown -R yuemo:yuemo /app

USER yuemo

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "/app/yuemo-mall.jar"]
