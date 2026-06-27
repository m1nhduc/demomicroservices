# Multi-stage build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
COPY common common/
COPY api-gateway api-gateway/
RUN mvn clean package -pl api-gateway -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/api-gateway/target/api-gateway-*.jar api-gateway.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "api-gateway.jar"]
