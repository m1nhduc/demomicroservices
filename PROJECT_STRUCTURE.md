# Project Structure

```
demomicroservices/
├── common/                              # Shared DTOs, Events, Constants
│   ├── pom.xml
│   └── src/main/java/dmd/prj/common/
│       ├── event/
│       │   ├── OrderPlacedEvent.java
│       │   ├── RedeemCommandEvent.java
│       │   ├── RedeemResultEvent.java
│       │   └── NotificationEvent.java
│       └── constant/
│           └── RabbitMQConstant.java
│
├── config-server/                       # Spring Cloud Config Server
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/...
│
├── eureka-server/                       # Service Discovery (Eureka)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/...
│
├── api-gateway/                         # API Gateway (Spring Cloud Gateway)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/...
│
├── auth-service/                        # Authentication Service (JWT + Redis)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/dmd/prj/authservice/
│       │   ├── AuthServiceApplication.java
│       │   ├── domain/User.java
│       │   ├── repository/UserRepository.java
│       │   ├── service/AuthService.java
│       │   ├── controller/AuthController.java
│       │   └── dto/{LoginRequest, LoginResponse}.java
│       └── resources/
│           ├── application.yml
│           └── db/changelog/
│               ├── db.changelog-master.yaml
│               └── 001-create-users-table.yaml
│
├── order-service/                       # Order Service (Outbox Pattern)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/dmd/prj/orderservice/
│       │   ├── OrderServiceApplication.java
│       │   ├── domain/{Order, Outbox}.java
│       │   ├── repository/{OrderRepository, OutboxRepository}.java
│       │   ├── service/OrderService.java
│       │   ├── controller/OrderController.java
│       │   ├── config/RabbitMQConfig.java
│       │   └── dto/{CreateOrderRequest, CreateOrderResponse}.java
│       └── resources/
│           ├── application.yml
│           └── db/changelog/
│               ├── db.changelog-master.yaml
│               └── 001-create-order-tables.yaml
│
├── coupon-service/                      # Coupon Service (Core Logic)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/dmd/prj/couponservice/
│       │   ├── CouponServiceApplication.java
│       │   ├── domain/{Coupon, TransactionTracking, Outbox}.java
│       │   ├── repository/{CouponRepository, TransactionTrackingRepository, OutboxRepository}.java
│       │   ├── service/{CouponService, OutboxRelayService, RecoveryJobService}.java
│       │   ├── config/RabbitMQConfig.java
│       │   ├── job/RecoveryJob.java
│       │   ├── listener/{OrderEventListener, RedeemResultListener}.java
│       │   └── dto/{CouponResponse, TransactionResponse}.java
│       └── resources/
│           ├── application.yml
│           └── db/changelog/
│               ├── db.changelog-master.yaml
│               └── 001-create-coupon-tables.yaml
│
├── notification-service/                # Notification Service
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/dmd/prj/notificationservice/
│       │   ├── NotificationServiceApplication.java
│       │   ├── domain/Notification.java
│       │   ├── repository/NotificationRepository.java
│       │   ├── service/NotificationService.java
│       │   ├── config/RabbitMQConfig.java
│       │   ├── listener/NotificationEventListener.java
│       │   └── controller/NotificationController.java
│       └── resources/
│           ├── application.yml
│           └── db/changelog/
│               ├── db.changelog-master.yaml
│               └── 001-create-notification-tables.yaml
│
├── pom.xml                              # Root POM (parent for all modules)
├── docker-compose.yml                   # Docker Compose for local development
├── init-databases.sql                   # PostgreSQL database initialization
├── BUILD.md                             # Build and deployment instructions
├── ARCHITECTURE.md                      # Architecture overview
├── .gitignore
└── README.md                            # Technical design document

```

## Module Dependencies

```
common
  ↓
auth-service, order-service, coupon-service, notification-service
  ↓
api-gateway
  ↓
eureka-server
  ↓
config-server
```

## Database Tables

### auth_db
- users (user_id, created_at)

### order_db
- orders (id, txn_id, user_id, product_type, amount, market, created_at)
- outbox (id, aggregate_type, aggregate_id, event_type, payload, status, retry_count, last_error, last_attempted_at, created_at)

### coupon_db
- coupons (id, user_id, status, reward_amount, expired_at, conditions, created_at)
- transaction_tracking (txn_id, user_id, coupon_id, status, payload, retry_count, last_error, created_at, updated_at)
- outbox (id, aggregate_type, aggregate_id, event_type, payload, status, retry_count, last_error, last_attempted_at, created_at)

### notification_db
- notifications (id, dedup_key, user_id, type, title, message, status, created_at)

## RabbitMQ Queues

| Exchange | Queue | Consumer | Retry | DLQ |
|----------|-------|----------|-------|-----|
| order.exchange | order.queue | coupon-service | order.retry.queue | order.dlq.queue |
| redeem.command.exchange | redeem.command.queue | wallet-mock (order-service) | - | redeem.command.dlq.queue |
| redeem.result.exchange | redeem.result.queue | coupon-service | - | redeem.result.dlq.queue |
| notification.exchange | notification.queue | notification-service | - | notification.dlq.queue |

## Key Implementation Details

### TODO: Services to Complete

- [ ] **CouponService**: Implement coupon matching logic
- [ ] **OutboxRelayService**: Implement periodic outbox relay job (5-second interval)
- [ ] **RecoveryJob**: Implement recovery job (5-minute interval) for failed transactions
- [ ] **OrderEventListener**: Implement consumer for order.queue
- [ ] **RedeemResultListener**: Implement consumer for redeem.result.queue
- [ ] **NotificationEventListener**: Implement consumer for notification.queue
- [ ] **Wallet Mock**: Implement in order-service to consume redeem.command.queue
- [ ] Add metrics and monitoring
- [ ] Add integration tests
