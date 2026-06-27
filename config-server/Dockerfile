# Multi-stage build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
COPY common common/
COPY config-server config-server/
RUN mvn clean package -pl config-server -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/config-server/target/config-server-*.jar config-server.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "config-server.jar"]
