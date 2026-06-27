# Multi-stage build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
COPY common common/
COPY auth-service auth-service/
RUN mvn clean package -pl auth-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/auth-service/target/auth-service-*.jar auth-service.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "auth-service.jar"]
