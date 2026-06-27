# Multi-stage build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
COPY common common/
COPY coupon-service coupon-service/
RUN mvn clean package -pl coupon-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/coupon-service/target/coupon-service-*.jar coupon-service.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "coupon-service.jar"]
