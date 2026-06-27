# Multi-stage build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
COPY common common/
COPY notification-service notification-service/
RUN mvn clean package -pl notification-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/notification-service/target/notification-service-*.jar notification-service.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "notification-service.jar"]
