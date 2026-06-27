# Coupon Matching & Redemption System - Build Instructions

## Prerequisites
- Docker & Docker Compose
- Java 21
- Maven 3.9+

## Quick Start

### 1. Build the entire project
```bash
mvn clean package -DskipTests
```

### 2. Build Docker images and start services
```bash
docker-compose up -d
```

### 3. Verify services are running
```bash
# Check all containers
docker-compose ps

# Check service health
curl http://localhost:8761/eureka/web  # Eureka Server
curl http://localhost:8080              # API Gateway
curl http://localhost:15672             # RabbitMQ Management (guest/guest)
```

## Architecture Overview

### Services
- **Config Server** (8888): Centralized configuration management
- **Eureka Server** (8761): Service discovery
- **API Gateway** (8080): Request routing and authentication
- **Auth Service** (8081): User authentication with JWT
- **Order Service** (8082): Order creation and event publishing
- **Coupon Service** (8083): Coupon matching and redemption
- **Notification Service** (8084): Event-driven notifications

### Infrastructure
- **PostgreSQL**: 4 databases (auth_db, order_db, coupon_db, notification_db)
- **Redis**: Session and cache storage
- **RabbitMQ**: Message broker for async communication

## Development

### Running individual service locally
```bash
# Auth Service
mvn spring-boot:run -pl auth-service

# Order Service
mvn spring-boot:run -pl order-service

# Coupon Service
mvn spring-boot:run -pl coupon-service

# Notification Service
mvn spring-boot:run -pl notification-service
```

### Testing the system

#### 1. Login to get token
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"userId":"USER_123"}'
```

Response:
```json
{
  "accessToken": "xxx",
  "refreshToken": "yyy"
}
```

#### 2. Create an order
```bash
curl -X POST http://localhost:8080/orders \
  -H "Authorization: Bearer xxx" \
  -H "X-User-Id: USER_123" \
  -H "Content-Type: application/json" \
  -d '{
    "productType": "STOCK",
    "amount": 100.00,
    "market": "NYSE"
  }'
```

Response:
```json
{
  "txnId": "TXN_STOCK_...",
  "status": "ACCEPTED"
}
```

## Key Features

### Event-Driven Architecture
- Transactional Outbox pattern for message reliability
- Choreography-based SAGA pattern for multi-service transactions
- At-least-once delivery guarantees

### Idempotency & Recovery
- All operations are idempotent
- Automatic recovery job for failed transactions
- No double-spending even with system crashes

### Observability
- Structured logging with correlation IDs
- Spring Cloud service discovery
- Configurable via Spring Cloud Config

## Database Schema

See `README.md` section "4. DATABASE SCHEMA" for detailed schema definitions.

### Liquibase Migrations
All services use Liquibase for database versioning:
- `src/main/resources/db/changelog/db.changelog-master.yaml`

## RabbitMQ Topology

See `README.md` section "5. RABBITMQ TOPOLOGY" for exchange/queue configurations.

### Exchanges
- `order.exchange` → `order.queue` (Coupon Service consumer)
- `redeem.command.exchange` → `redeem.command.queue` (Wallet Mock consumer)
- `redeem.result.exchange` → `redeem.result.queue` (Coupon Service consumer)
- `notification.exchange` → `notification.queue` (Notification Service consumer)

## Troubleshooting

### Services won't start
1. Check Docker is running: `docker --version`
2. Check port conflicts: `docker-compose down`
3. Check logs: `docker-compose logs -f service-name`

### Database connection errors
1. Wait for postgres to be healthy: `docker-compose ps`
2. Check migrations: `docker-compose logs order-service | grep liquibase`

### RabbitMQ connection errors
1. Check rabbitmq is running: `curl http://localhost:15672`
2. Default credentials: guest/guest
3. Reset: `docker-compose exec rabbitmq rabbitmqctl reset`

## Next Steps

1. Implement coupon matching logic in `CouponService`
2. Implement Outbox relay job for message publishing
3. Implement Recovery Job for failed transactions
4. Add integration tests for end-to-end flows
5. Deploy to Kubernetes

## References

- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)
- [Liquibase Documentation](https://docs.liquibase.com/)
