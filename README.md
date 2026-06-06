# TECHNICAL DESIGN DOCUMENT
## Coupon Matching & Redemption System
**Kiến trúc:** Event-Driven | Transactional Outbox | Choreography SAGA | Idempotency

---

## 1. TỔNG QUAN BÀI TOÁN

Hệ thống nhận event thanh toán thành công từ hệ thống tài chính (Stock/FX/ETF), thực hiện:
1. Match transaction với coupon phù hợp nhất của user (ưu tiên coupon sắp hết hạn).
2. Thực hiện Redeem bằng cách gửi command sang Wallet Service qua RabbitMQ.
3. Cập nhật trạng thái dựa trên kết quả từ Wallet.

**Yêu cầu phi chức năng:**
- Tuần tự theo từng user (không xử lý song song 2 transaction của cùng 1 user).
- Không double-spending dù hệ thống crash ở bất kỳ bước nào.
- Tự phục hồi sau crash (Recovery Job).
- Observable: structured logging, metrics, distributed tracing.

---

## 2. TECH STACK

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Database | PostgreSQL (mỗi service một DB riêng) |
| DB Versioning | Liquibase |
| Cache / Lock | Redis |
| Message Broker | RabbitMQ |
| API Gateway | Spring Cloud Gateway + Resilience4j |
| Service Discovery | Eureka (Spring Cloud Netflix) |
| Config Server | Spring Cloud Config (Git backend) |
| Auth | Spring Security + JWT (stateful, Redis-backed) |
| Circuit Breaker | Resilience4j |
| Reverse Proxy | Nginx |
| Logs | Loki |
| Metrics | Prometheus + Micrometer |
| Traces | Tempo + OpenTelemetry |
| UI Monitoring | Grafana |
| Container | Docker Compose |

---

## 3. DANH SÁCH MICROSERVICES

### Services tự build:
| Service | Vai trò |
|---|---|
| `nginx` | Reverse proxy, TLS termination, north-south entry point |
| `api-gateway` | Routing, token validation, circuit breaker |
| `config-server` | Centralized config từ Git repo |
| `eureka-server` | Service discovery |
| `auth-service` | Register, login, logout, refresh token |
| `coupon-service` | Core: matching, redeem, recovery job |
| `order-service` | Tạo order, publish event; bao gồm Wallet Mock |
| `notification-service` | Lưu trữ notification cho user và admin |

### External systems (mock):
- **Payment/Order Source** → được thay bằng `order-service`.
- **Wallet Service** → được mock bên trong `order-service` (happy path).

### Communication pattern:
- **North-South (Client → Services):** `Client → Nginx → API Gateway → Service`
- **East-West (Service → Service):** Internal Docker DNS trực tiếp (ví dụ: `http://auth-service:8080`), không đi qua Gateway.
- **Async:** RabbitMQ với Transactional Outbox pattern.

---

## 4. DATABASE SCHEMA

### 4.1. `auth-service` DB

```sql
-- Bảng users
CREATE TABLE users (
    user_id     VARCHAR(64) PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
```

Redis keys (Auth):
- `access_token:{user_id}` → JWT access token (TTL: 15 phút)
- `refresh_token:{user_id}` → Refresh token (TTL: 7 ngày)

### 4.2. `order-service` DB

```sql
-- Bảng orders
CREATE TABLE orders (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    txn_id       VARCHAR(128) NOT NULL UNIQUE,  -- Idempotency key
    user_id      VARCHAR(64) NOT NULL,
    product_type VARCHAR(16) NOT NULL,           -- STOCK, FX, ETF
    amount       NUMERIC(19, 4) NOT NULL,
    market       VARCHAR(32) NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Bảng wallet_transactions (Wallet Mock idempotency guard)
CREATE TABLE wallet_transactions (
    txn_id     VARCHAR(128) PRIMARY KEY,         -- Idempotency key từ REDEEM_COMMAND
    user_id    VARCHAR(64) NOT NULL,
    coupon_id  BIGINT NOT NULL,
    amount     NUMERIC(19, 4) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Outbox table (Order Service)
CREATE TABLE outbox (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id   VARCHAR(128) NOT NULL,
    event_type     VARCHAR(64) NOT NULL,          -- ORDER_PLACED, REDEEM_RESULT
    payload        JSONB NOT NULL,
    status         VARCHAR(16) NOT NULL DEFAULT 'PENDING',  -- PENDING, SENT
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_outbox_status ON outbox(status, created_at) WHERE status = 'PENDING';
```

### 4.3. `coupon-service` DB

```sql
-- Bảng coupons
CREATE TABLE coupons (
    id          BIGSERIAL PRIMARY KEY,
    user_id     VARCHAR(64) NOT NULL,
    status      VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
                -- AVAILABLE, PENDING, REDEEMED, EXPIRED
    expired_at  TIMESTAMP,                        -- NULL = không bao giờ hết hạn
    conditions  JSONB,                            -- Dynamic matching rules
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_coupons_user_status ON coupons(user_id, status);
CREATE INDEX idx_coupons_expired_at  ON coupons(expired_at ASC NULLS LAST);

-- Bảng transaction_tracking (Idempotency + Outbox state)
CREATE TABLE transaction_tracking (
    txn_id      VARCHAR(128) PRIMARY KEY,        -- Idempotency key từ order
    user_id     VARCHAR(64) NOT NULL,
    coupon_id   BIGINT REFERENCES coupons(id),
    status      VARCHAR(16) NOT NULL DEFAULT 'INIT',
                -- INIT, SUCCESS, FAILED
    payload     JSONB NOT NULL,                  -- Dữ liệu để replay nếu cần
    retry_count INT NOT NULL DEFAULT 0,          -- Max: 5
    last_error  TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_tracking_status_created ON transaction_tracking(status, created_at)
    WHERE status = 'INIT';

-- Outbox table (Coupon Service)
CREATE TABLE outbox (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id   VARCHAR(128) NOT NULL,
    event_type     VARCHAR(64) NOT NULL,
                   -- REDEEM_COMMAND, NOTIFICATION_EVENT
    payload        JSONB NOT NULL,
    status         VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_outbox_status ON outbox(status, created_at) WHERE status = 'PENDING';
```

### 4.4. `notification-service` DB

```sql
-- Bảng notifications
CREATE TABLE notifications (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    VARCHAR(64) NOT NULL,
    type       VARCHAR(32) NOT NULL,
               -- REDEEM_FAILED, REDEEM_SUCCESS, ADMIN_ALERT
    title      VARCHAR(256) NOT NULL,
    message    TEXT NOT NULL,
    status     VARCHAR(16) NOT NULL DEFAULT 'UNREAD', -- UNREAD, READ
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notifications_user ON notifications(user_id, status);
```

---

## 5. RABBITMQ TOPOLOGY

### Exchanges & Queues:

```
# 1. Order flow: Order Service → Coupon Service
order.exchange (direct)
  └─> routing_key: order.placed
        └─> order.queue                   [Coupon Service consumes]

order.retry.exchange (direct)
  └─> order.retry.queue                   [TTL=5000ms, x-dead-letter-exchange=order.exchange]

order.dlq.exchange (direct)
  └─> order.dlq.queue                     [Manual intervention]

# 2. Redeem Command: Coupon Service → Wallet Mock (in Order Service)
redeem.command.exchange (direct)
  └─> routing_key: redeem.command
        └─> redeem.command.queue          [Wallet Mock consumes]

redeem.command.dlq.exchange (direct)
  └─> redeem.command.dlq.queue

# 3. Redeem Result: Wallet Mock → Coupon Service
redeem.result.exchange (direct)
  └─> routing_key: redeem.result
        └─> redeem.result.queue           [Coupon Service consumes]

# 4. Notification: Coupon Service → Notification Service
notification.exchange (direct)
  └─> routing_key: notification.event
        └─> notification.queue            [Notification Service consumes]

notification.dlq.exchange (direct)
  └─> notification.dlq.queue
```

### Event Payloads:

**ORDER_PLACED** (Order Service → Coupon Service):
```json
{
  "txn_id": "TXN_STOCK_20240101_001",
  "user_id": "USER_123",
  "product_type": "FX",
  "amount": 150.00,
  "market": "USD/VND",
  "created_at": "2024-01-01T10:00:00Z"
}
```

**REDEEM_COMMAND** (Coupon Service → Wallet Mock):
```json
{
  "txn_id": "TXN_STOCK_20240101_001",
  "user_id": "USER_123",
  "coupon_id": 42,
  "amount": 50000,
  "created_at": "2024-01-01T10:00:01Z"
}
```

**REDEEM_RESULT** (Wallet Mock → Coupon Service):
```json
{
  "txn_id": "TXN_STOCK_20240101_001",
  "success": true,
  "error_code": null
}
```
Hoặc khi lỗi:
```json
{
  "txn_id": "TXN_STOCK_20240101_001",
  "success": false,
  "error_code": "WALLET_LOCKED"
}
```

**NOTIFICATION_EVENT** (Coupon Service → Notification Service):
```json
{
  "user_id": "USER_123",
  "type": "REDEEM_FAILED",
  "title": "Redeem thất bại",
  "message": "Coupon #42 không thể áp dụng. Lý do: WALLET_LOCKED",
  "admin_alert": true
}
```

---

## 6. LUỒNG XỬ LÝ CHI TIẾT

### 6.1. Auth Flow

**Login:**
1. Client POST `/auth/login` với `{user_id}`.
2. Auth Service tạo access token (JWT, TTL 15 phút) và refresh token (TTL 7 ngày).
3. Lưu cả 2 token vào Redis:
    - `SET access_token:{user_id} {token} EX 900`
    - `SET refresh_token:{user_id} {token} EX 604800`
4. Trả về `{access_token, refresh_token}`.

**Validate (mỗi request qua Gateway):**
1. API Gateway extract Bearer token từ `Authorization` header.
2. Gọi Auth Service (`http://auth-service:8080/auth/validate`) qua internal DNS.
3. Auth Service check token trong Redis còn tồn tại không (stateful check).
4. Nếu valid → trả `{user_id}`, Gateway forward request kèm header `X-User-Id: {user_id}`.
5. Nếu invalid → Gateway trả 401.

**Refresh Token:**
1. Client POST `/auth/refresh` với refresh token.
2. Auth Service verify refresh token trong Redis.
3. Tạo access token mới, cập nhật Redis.

**Logout:**
1. Client POST `/auth/logout`.
2. Auth Service xóa `access_token:{user_id}` và `refresh_token:{user_id}` khỏi Redis.

---

### 6.2. Order Flow (Order Service)

1. Client POST `/orders` với `{product_type, amount, market}`.
2. Order Service:
    - Tạo `txn_id` unique (ví dụ: `TXN_{product_type}_{timestamp}_{uuid}`).
    - Insert vào bảng `orders`.
    - Trong **cùng một DB transaction**: Insert vào bảng `outbox` với `event_type=ORDER_PLACED`.
    - COMMIT.
3. Trả về `{txn_id, status: "ACCEPTED"}` cho client ngay lập tức.
4. **Outbox Relay** (`@Scheduled`, mỗi 5 giây):
    - Query `SELECT * FROM outbox WHERE status='PENDING' ORDER BY created_at ASC FOR UPDATE SKIP LOCKED`.
    - `SKIP LOCKED` đảm bảo an toàn khi chạy multi-instance — mỗi instance chỉ xử lý các record chưa bị instance khác lock.
    - Publish từng record lên `order.exchange`.
    - Update `outbox.status = 'SENT'`.

---

### 6.3. Coupon Service — Main Consumer Flow

**Consumer 1:** Lắng nghe `order.queue`

**Giai đoạn 1: Locking (Smart Back-off)**

```
1. Thử Redis Lock: SET user_lock:{user_id} {uuid} NX EX 20
2. Nếu thất bại: sleep(200ms), thử lại tối đa 3 lần
3. Nếu vẫn thất bại sau 3 lần:
   a. Kiểm tra x-death header count
   b. Nếu count < MAX_REQUEUE (ví dụ: 10):
      - Publish event sang order.retry.exchange (TTL=5s, tự động về order.queue)
      - ACK message hiện tại
   c. Nếu count >= MAX_REQUEUE:
      - Publish sang order.dlq.queue
      - ACK message hiện tại
      - Alert Admin
4. Release lock nếu thất bại hoàn toàn
```

**Giai đoạn 2: Atomic DB Transaction**

```
Mở DB Transaction:

1. SELECT * FROM coupons
   WHERE user_id = ? AND status = 'AVAILABLE'
   ORDER BY expired_at ASC NULLS LAST
   FOR UPDATE

2. [MATCHING LOGIC — để comment, tự implement sau]
   Điều kiện: So sánh transaction (product_type, amount, market)
   với coupon.conditions (JSONB)
   Ưu tiên: expired_at ASC NULLS LAST (hết hạn sớm trước, không hết hạn sau cùng)
   Kết quả: selectedCoupon hoặc null

3. Nếu selectedCoupon == null:
   - Rollback
   - Release Redis Lock
   - ACK message
   - RETURN (silent skip)

4. INSERT INTO transaction_tracking
   (txn_id, user_id, coupon_id, status, payload)
   VALUES (?, ?, ?, 'INIT', ?)
   -- Nếu DuplicateKeyException:
   --   Rollback, Release Redis Lock, ACK, RETURN (đã xử lý rồi)

5. INSERT INTO outbox
   (aggregate_type, aggregate_id, event_type, payload)
   VALUES ('coupon', txn_id, 'REDEEM_COMMAND', {txn_id, user_id, coupon_id, amount})

6. UPDATE coupons SET status = 'REDEEMED' WHERE id = selectedCoupon.id

7. COMMIT

8. Release Redis Lock (dùng Lua script để đảm bảo chỉ owner mới release)

9. ACK message về Broker
```

**Giai đoạn 3: Outbox Relay** (`@Scheduled`, mỗi 5 giây)
```
SELECT * FROM outbox WHERE status='PENDING' ORDER BY created_at ASC FOR UPDATE SKIP LOCKED
Với mỗi record:
  - Nếu event_type == 'REDEEM_COMMAND': publish lên redeem.command.exchange
  - Nếu event_type == 'NOTIFICATION_EVENT': publish lên notification.exchange
  - Update outbox.status = 'SENT'
```

---

### 6.4. Coupon Service — Wallet Result Consumer

**Consumer 2:** Lắng nghe `redeem.result.queue`

```
Nhận REDEEM_RESULT message {txn_id, success, error_code}:

Nếu success == true:
  UPDATE transaction_tracking SET status='SUCCESS', updated_at=NOW()
  WHERE txn_id = ?

Nếu success == false (error_code != null):
  Mở DB Transaction:
    UPDATE transaction_tracking
    SET status='FAILED', last_error=error_code, updated_at=NOW()
    WHERE txn_id = ?

    UPDATE coupons SET status='PENDING'
    WHERE id = (SELECT coupon_id FROM transaction_tracking WHERE txn_id = ?)

    INSERT INTO outbox (event_type='NOTIFICATION_EVENT', payload={
      user_id, type='REDEEM_FAILED',
      title='Redeem thất bại',
      message='Coupon không thể áp dụng. Lý do: {error_code}'
      admin_alert: true
    })
  COMMIT

ACK message
```

---

### 6.5. Recovery Job (Coupon Service)

`@Scheduled(fixedDelay = 300000)` — mỗi 5 phút

```
SELECT * FROM transaction_tracking
WHERE status = 'INIT'
  AND retry_count < 5
  AND created_at < NOW() - INTERVAL '5 minutes'
FOR UPDATE SKIP LOCKED

Với mỗi bản ghi:

  Mở DB Transaction:
    UPDATE transaction_tracking
    SET retry_count = retry_count + 1, updated_at = NOW()
    WHERE txn_id = ?

    INSERT INTO outbox (event_type='REDEEM_COMMAND', payload=tracking.payload)

    Nếu retry_count + 1 >= 5:
      UPDATE transaction_tracking SET status='FAILED'
      UPDATE coupons SET status='PENDING' WHERE id = tracking.coupon_id
      INSERT INTO outbox (event_type='NOTIFICATION_EVENT', payload={
        type='ADMIN_ALERT',
        message='txn_id {txn_id} đã retry 5 lần, cần can thiệp thủ công'
      })
  COMMIT
```

---

### 6.6. Wallet Mock Flow (trong Order Service)

**Consumer:** Lắng nghe `redeem.command.queue`

```
Nhận REDEEM_COMMAND {txn_id, user_id, coupon_id, amount}:

[Happy path — luôn thành công]

Mở DB Transaction:
  -- Idempotency guard: nếu txn_id đã tồn tại thì bỏ qua (đã xử lý rồi)
  INSERT INTO wallet_transactions (txn_id, user_id, coupon_id, amount)
  VALUES (?, ?, ?, ?)
  ON CONFLICT (txn_id) DO NOTHING
  -- Nếu bị conflict (duplicate): ACK và RETURN

  INSERT INTO outbox (aggregate_type, aggregate_id, event_type, payload)
  VALUES ('wallet', txn_id, 'REDEEM_RESULT', {
    txn_id: txn_id,
    success: true,
    error_code: null
  })
COMMIT

ACK message
```

---

### 6.7. Notification Service Flow

**Consumer:** Lắng nghe `notification.queue`

```
Nhận NOTIFICATION_EVENT {user_id, type, title, message, admin_alert}:

INSERT INTO notifications (user_id, type, title, message, status='UNREAD')

Nếu admin_alert == true:
  [Placeholder: gửi Telegram/Slack webhook — implement sau]

ACK message
```

---

### 6.8. Coupon Expiry Job (Coupon Service)

`@Scheduled(cron = "0 0 1 * * *")` — chạy lúc 1:00 AM mỗi ngày

```
UPDATE coupons
SET status = 'EXPIRED'
WHERE expired_at < NOW()
  AND status = 'AVAILABLE'

Log số lượng coupon vừa bị expire.
```

**Lưu ý:** Chỉ expire coupon ở trạng thái `AVAILABLE`. Coupon đang ở `PENDING` (đang chờ admin xử lý) không bị ảnh hưởng.

---

## 7. AUTH SERVICE — ENDPOINT LIST

Tất cả endpoints đều qua Nginx → API Gateway.
Riêng các endpoint `/auth/**` là **public** (không cần token).

| Method | Path | Auth Required | Mô tả |
|---|---|---|---|
| POST | `/auth/register` | No | Tạo user mới (chỉ cần user_id) |
| POST | `/auth/login` | No | Đăng nhập, nhận token |
| POST | `/auth/logout` | Yes | Xóa token khỏi Redis |
| POST | `/auth/refresh` | No | Lấy access token mới |
| GET | `/auth/validate` | Internal only | Gateway gọi để validate token |

---

## 8. COUPON SERVICE — ENDPOINT LIST

Tất cả cần `X-User-Id` header (được Gateway inject sau khi validate token).

| Method | Path | Mô tả |
|---|---|---|
| GET | `/coupons` | Lấy danh sách coupon của user |
| GET | `/coupons/{id}` | Chi tiết một coupon |
| GET | `/transactions` | Lịch sử transaction_tracking của user |

---

## 9. ORDER SERVICE — ENDPOINT LIST

| Method | Path | Mô tả |
|---|---|---|
| POST | `/orders` | Tạo order mới |
| GET | `/orders` | Lịch sử order của user |

---

## 10. NOTIFICATION SERVICE — ENDPOINT LIST

| Method | Path | Mô tả |
|---|---|---|
| GET | `/notifications` | Danh sách notification của user |
| PATCH | `/notifications/{id}/read` | Đánh dấu đã đọc |

---

## 11. REDIS KEY DESIGN

| Key | TTL | Mục đích |
|---|---|---|
| `access_token:{user_id}` | 15 phút | Stateful auth |
| `refresh_token:{user_id}` | 7 ngày | Stateful auth |
| `user_lock:{user_id}` | 20 giây | Distributed lock (coupon processing) |

**Distributed Lock release** dùng Lua script (atomic check-and-delete):
```lua
if redis.call('get', KEYS[1]) == ARGV[1] then
  return redis.call('del', KEYS[1])
else
  return 0
end
```

---

## 12. TRANSACTIONAL OUTBOX PATTERN

Áp dụng cho tất cả services có publish event:
- `order-service`: publish ORDER_PLACED, REDEEM_RESULT
- `coupon-service`: publish REDEEM_COMMAND, NOTIFICATION_EVENT

**Nguyên tắc:**
- Outbox table nằm trong **cùng DB** với business table của service đó.
- Insert vào outbox trong **cùng DB transaction** với business operation.
- `@Scheduled` relay thread đọc outbox với `FOR UPDATE SKIP LOCKED` — đảm bảo an toàn khi chạy multi-instance, tránh duplicate publish.
- Nếu publish thành công thì update `status=SENT`. Nếu service crash giữa chừng, record vẫn ở `PENDING`, lần chạy tiếp theo sẽ publish lại.
- **Idempotency** phía consumer đảm bảo duplicate message không gây duplicate processing.
- **Cleanup:** Một `@Scheduled` job chạy hàng ngày xóa các record `SENT` cũ hơn 7 ngày để tránh outbox table phình to.

---

## 13. API GATEWAY CONFIG

- **Routing với path rewriting:**
    - `/api/auth/**` → strip prefix `/api` → forward đến `http://auth-service:8081`
    - `/api/orders/**` → strip prefix `/api` → forward đến `http://order-service:8083`
    - `/api/coupons/**` → strip prefix `/api` → forward đến `http://coupon-service:8082`
    - `/api/notifications/**` → strip prefix `/api` → forward đến `http://notification-service:8084`
    - Ví dụ: client gọi `GET /api/coupons` → gateway forward thành `GET /coupons` đến coupon-service.
- **Token Validation Filter (Global Filter):** Với mọi request không phải `/api/auth/login`, `/api/auth/register`, `/api/auth/refresh` → gọi `http://auth-service:8081/auth/validate`, inject `X-User-Id` vào request header trước khi forward.
- **Circuit Breaker (Resilience4j):** Wrap từng route, fallback trả 503 với body `{"error": "Service temporarily unavailable"}`.
- **Service Discovery:** Dùng Eureka để resolve — có thể dùng `lb://service-name` trong route URI thay vì hardcode host:port.

---

## 14. MONITORING & OBSERVABILITY

### Structured Logging
Mọi log line phải có:
```json
{
  "timestamp": "...",
  "level": "INFO",
  "service": "coupon-service",
  "trace_id": "...",
  "span_id": "...",
  "user_id": "...",
  "txn_id": "...",
  "coupon_id": "...",
  "message": "..."
}
```
Stack: Logback + Logstash Logback Encoder → Loki.

### Metrics (Micrometer → Prometheus)
Business metrics cần có:
- `coupon.redeem.success.total` (counter)
- `coupon.redeem.failed.total` (counter, tag: `reason`)
- `coupon.match.miss.total` (counter — không có coupon nào match)
- `transaction.tracking.init.count` (gauge — số bản ghi INIT đang tồn đọng)
- `coupon.pending.count` (gauge — số coupon ở trạng thái PENDING)
- `recovery.job.processed.total` (counter)
- `wallet.call.latency` (timer)

### Distributed Tracing
- Stack: Micrometer Tracing + OpenTelemetry → Tempo.
- `trace_id` phải được propagate qua:
    - RabbitMQ message header (`X-B3-TraceId`)
    - HTTP header `traceparent` khi gọi internal service
    - Log MDC để correlate log với trace

### Alerting Rules
- `transaction.tracking.init.count > 100` trong 10 phút → Wallet Service có vấn đề.
- `coupon.pending.count` tăng > 10 trong 1 giờ → Admin cần kiểm tra.
- Recovery Job không chạy trong 15 phút → Job chết.
- Retry queue depth > 50 → Lock contention cao bất thường.

---

## 15. INFRASTRUCTURE — DOCKER COMPOSE SERVICES

### Service list & Port mapping:

| Container | Internal Port | Expose (dev) | Ghi chú |
|---|---|---|---|
| `nginx` | 80 / 443 | 80 / 443 | Entry point duy nhất từ bên ngoài |
| `api-gateway` | 8080 | — | Internal only |
| `config-server` | 8888 | 8888 | Expose để debug config |
| `eureka-server` | 8761 | 8761 | Expose để xem dashboard |
| `auth-service` | 8081 | — | Internal only |
| `coupon-service` | 8082 | — | Internal only |
| `order-service` | 8083 | — | Internal only |
| `notification-service` | 8084 | — | Internal only |
| `postgres` | 5432 | 5432 | **1 instance, 4 databases** (xem bên dưới) |
| `redis` | 6379 | 6379 | Expose để debug |
| `rabbitmq` | 5672 | 15672 | Expose Management UI port |
| `prometheus` | 9090 | 9090 | — |
| `loki` | 3100 | — | Internal only |
| `tempo` | 3200 | — | Internal only |
| `grafana` | 3000 | 3000 | — |

### PostgreSQL — 1 instance, 4 databases:
Dùng **1 PostgreSQL container** với 4 databases riêng biệt để tiết kiệm RAM:
- `auth_db` — cho auth-service
- `order_db` — cho order-service
- `coupon_db` — cho coupon-service
- `notification_db` — cho notification-service

Mỗi service có connection string trỏ đến database của mình: `jdbc:postgresql://postgres:5432/{service}_db`.

---

## 16. LƯU Ý QUAN TRỌNG KHI CODE

1. **Liquibase + Hibernate — phân công rõ ràng:**
    - `spring.jpa.hibernate.ddl-auto=validate` — Hibernate **chỉ validate** entity mapping với schema hiện tại, **không được** tạo hay sửa bảng.
    - Liquibase chịu trách nhiệm **toàn bộ DDL**: tạo bảng, tạo index, seed data ban đầu, mọi migration sau này.
    - Thứ tự khởi động: Liquibase chạy migration trước → Hibernate validate → nếu schema không khớp entity thì app fail ngay lúc start.
    - Mỗi service có thư mục `src/main/resources/db/changelog` riêng. File master: `db.changelog-master.yaml`.

2. **Manual ACK — bắt buộc cho tất cả consumers:**
    - Config Spring AMQP: `spring.rabbitmq.listener.simple.acknowledge-mode=manual`.
    - KHÔNG dùng auto-ack. Consumer phải gọi `channel.basicAck()` tường minh sau khi xử lý xong.
    - Trong mọi trường hợp (kể cả exception), consumer phải ACK hoặc NACK — không để message bị treo.

3. **Config Server — cấu trúc git repo:**
    - Dùng một **git repo riêng** chỉ để chứa config (tách khỏi source code).
    - Cấu trúc: `auth-service.yml`, `coupon-service.yml`, `order-service.yml`, `notification-service.yml`, `api-gateway.yml`, `application.yml` (shared config cho tất cả).
    - Spring Boot 3.x không dùng `bootstrap.yml` nữa. Thay bằng `spring.config.import=configserver:http://config-server:8888` trong `application.yml` của mỗi service.
    - Dev fallback: `spring.cloud.config.fail-fast=false`.

4. **Eureka — tắt self-preservation trong dev:**
    - Thêm vào config của eureka-server: `eureka.server.enable-self-preservation=false`.
    - Lý do: self-preservation giữ lại service instance đã chết trong registry — gây confusing khi dev.

5. **Redis Lock:** Giá trị của lock phải là UUID được sinh ra **per-request** (không phải fixed string). UUID này được so sánh trong Lua script khi release để đảm bảo chỉ owner mới release được lock.

6. **Outbox Relay SKIP LOCKED:** Relay query phải dùng `FOR UPDATE SKIP LOCKED` — đảm bảo khi chạy multi-instance, mỗi instance chỉ xử lý các record chưa bị lock bởi instance khác, tránh duplicate publish.

7. **Idempotency:** Consumer luôn check trước khi xử lý. Coupon Service dùng PK của `transaction_tracking`. Wallet Mock dùng `wallet_transactions.txn_id` với `ON CONFLICT DO NOTHING`.

8. **x-death header:** Khi message bị re-queue qua DLX, RabbitMQ tự động thêm/tăng `x-death` header. Consumer đọc header này để biết số lần đã retry. Dùng `MessageProperties.getXDeathHeader()` trong Spring AMQP.

9. **Jakarta EE:** Spring Boot 3.x dùng `jakarta.*` thay vì `javax.*`. Chú ý khi import.

10. **Virtual Threads (Java 21):** Enable bằng `spring.threads.virtual.enabled=true` — hữu ích khi có nhiều blocking I/O.

11. **Matching Logic:** Để `// TODO: implement matching logic` tại bước 2 của Atomic Transaction. Input: `List<Coupon>` và `OrderEvent`. Output: `Optional<Coupon>`. Nếu empty → silent skip, release lock, ACK.

12. **NULLS LAST:** `ORDER BY expired_at ASC NULLS LAST` — coupon không có `expired_at` (không bao giờ hết hạn) luôn ở cuối danh sách ưu tiên.

13. **Spring profiles:** Dùng 2 profiles: `dev` (local docker-compose, fail-fast=false, verbose logging) và `prod` (strict config, fail-fast=true). Active profile set qua biến môi trường `SPRING_PROFILES_ACTIVE`.

14. **Docker Compose depends_on:** Các service phải chờ dependencies sẵn sàng thực sự (không chỉ container started). Dùng `depends_on` với `condition: service_healthy` và định nghĩa `healthcheck` cho PostgreSQL, Redis, RabbitMQ.