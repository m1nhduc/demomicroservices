# Multi-stage build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
COPY common common/
COPY order-service order-service/
RUN mvn clean package -pl order-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/order-service/target/order-service-*.jar order-service.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "order-service.jar"]
