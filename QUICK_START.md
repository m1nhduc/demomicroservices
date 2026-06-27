# Quick Reference Guide

## 🚀 Fast Start

### 1. Clone & Navigate
```bash
cd f:\prj\demomicroservices
```

### 2. Check Java Version (Must have Java 11+)
```bash
java -version
```
If you only have Java 8, use Docker instead:
```bash
docker-compose build
docker-compose up
```

### 3. Build All Modules (if Java 11+ available)
```bash
mvn clean package -DskipTests
```

### 4. Run with Docker
```bash
docker-compose up -d
```

### 5. Verify Services
```bash
# Eureka UI
curl http://localhost:8761/eureka/web

# RabbitMQ Management
curl http://localhost:15672  # guest/guest

# PostgreSQL
psql -h localhost -U postgres -d auth_db
```

## 📚 Key Files

| File | Purpose |
|------|---------|
| pom.xml | Root Maven POM with all modules |
| docker-compose.yml | Docker setup for local development |
| init-databases.sql | Initialize 4 databases |
| BUILD.md | Build & deployment instructions |
| PROJECT_STRUCTURE.md | Module & file structure overview |
| IMPLEMENTATION_STATUS.md | What's done & what's TODO |
| README.md | Technical design document |

## 🏗️ Architecture

```
Client → Nginx/API Gateway → Services → Databases
                            ↓
                         RabbitMQ (Async)
                            ↓
                     Other Services
```

### Services & Ports
- **API Gateway**: 8080
- **Auth Service**: 8081
- **Order Service**: 8082
- **Coupon Service**: 8083
- **Notification Service**: 8084
- **Eureka**: 8761
- **Config Server**: 8888

### Infrastructure
- **PostgreSQL**: 5432 (4 databases)
- **Redis**: 6379
- **RabbitMQ**: 5672, 15672

## 🔄 Workflow

1. **User authenticates**
   ```
   POST /auth/login → Auth Service → Returns JWT token
   ```

2. **User creates order**
   ```
   POST /orders → Order Service 
   → Saves order + Outbox event 
   → Publishes to order.exchange
   ```

3. **Coupon Service processes**
   ```
   Consumes order.queue 
   → Locks user (Redis) 
   → Matches coupon 
   → Publishes REDEEM_COMMAND
   ```

4. **Wallet Mock processes**
   ```
   Consumes redeem.command.queue 
   → Processes payment 
   → Publishes REDEEM_RESULT
   ```

5. **Coupon Service finishes**
   ```
   Consumes redeem.result.queue 
   → Updates coupon status 
   → Publishes notification
   ```

6. **Notification Service stores**
   ```
   Consumes notification.queue 
   → Saves notification
   ```

## 🛠️ Common Commands

### Development
```bash
# Compile only
mvn clean compile -DskipTests

# Run one service
mvn spring-boot:run -pl auth-service

# Run tests
mvn clean test

# Package for deployment
mvn clean package -DskipTests
```

### Docker
```bash
# Build images
docker-compose build

# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f service-name

# Clean up (remove data)
docker-compose down -v
```

### Database
```bash
# Connect to postgres
psql -h localhost -U postgres

# List databases
\l

# Connect to auth_db
\c auth_db

# List tables
\dt
```

### RabbitMQ
```bash
# Management UI
http://localhost:15672
# Username: guest
# Password: guest

# CLI commands
docker-compose exec rabbitmq rabbitmqctl list_queues
docker-compose exec rabbitmq rabbitmqctl list_exchanges
docker-compose exec rabbitmq rabbitmqctl list_bindings
```

## 📊 Database Schemas

### Auth DB
```sql
users (user_id, created_at)
```

### Order DB
```sql
orders (id, txn_id, user_id, product_type, amount, market, created_at)
outbox (id, aggregate_type, aggregate_id, event_type, payload, status, ...)
```

### Coupon DB
```sql
coupons (id, user_id, status, reward_amount, expired_at, conditions, ...)
transaction_tracking (txn_id, user_id, coupon_id, status, payload, ...)
outbox (id, aggregate_type, aggregate_id, event_type, payload, status, ...)
```

### Notification DB
```sql
notifications (id, dedup_key, user_id, type, title, message, status, ...)
```

## 🐛 Troubleshooting

### Build fails with "invalid target release"
```
Error: invalid target release: 11
Solution: Install Java 11 or use Docker to build
```

### Docker won't start
```
docker-compose down -v
docker-compose up -d
```

### RabbitMQ connection refused
```
Wait 10-15 seconds for RabbitMQ to start
docker-compose logs rabbitmq
```

### PostgreSQL connection refused
```
Check container is running: docker-compose ps
Reset: docker-compose down -v && docker-compose up -d
```

## 📝 Implementation TODOs

### Critical Path (Phase 1)
- [ ] CouponService.matchCoupon() - Coupon matching logic
- [ ] CouponService.redeem() - Redeem flow
- [ ] OutboxRelayService - Outbox relay job (5s)
- [ ] RecoveryJob - Recovery job (5min)
- [ ] Event Listeners - Order/Result/Notification consumers
- [ ] Wallet Mock - In order-service

### Important (Phase 2)
- [ ] Integration tests
- [ ] Metrics & monitoring
- [ ] Error handling & retries
- [ ] Distributed tracing

### Nice-to-have (Phase 3)
- [ ] Kubernetes manifests
- [ ] Helm charts
- [ ] CI/CD pipelines
- [ ] Performance optimization
- [ ] API documentation (Swagger)

## 📚 References

- [Spring Boot 3.3.4 Docs](https://spring.io/projects/spring-boot)
- [Spring Cloud 2023.0.3](https://spring.io/projects/spring-cloud)
- [RabbitMQ Tutorials](https://www.rabbitmq.com/getstarted.html)
- [PostgreSQL Docs](https://www.postgresql.org/docs/)
- [Docker Compose Docs](https://docs.docker.com/compose/)
- [Liquibase Docs](https://docs.liquibase.com/)

## 📞 Support

For questions or issues:
1. Check IMPLEMENTATION_STATUS.md for progress
2. Review BUILD.md for detailed setup
3. Check PROJECT_STRUCTURE.md for file organization
4. Read README.md for technical design

---

**Version**: 1.0.0-SNAPSHOT
**Status**: Core structure complete, ready for feature implementation
**Next**: Implement business logic in CouponService
