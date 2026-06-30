# RabbitMQ Configuration Fixes - Summary

## 🔍 Vấn đề phát hiện

### 1. **Coupon-Service RabbitMQConfig** - LỖI NGHIÊM TRỌNG
**File:** `coupon-service/src/main/java/dmd/prj/couponservice/config/RabbitMQConfig.java`

#### Các lỗi:
- ❌ **Khai báo sai**: Khai báo `redeemCommandExchange` và `redeemCommandQueue` 
  - Coupon-service chỉ **PUBLISH** vào queue này, không **CONSUME**
  - Queue này được CONSUME bởi Wallet Mock (trong order-service)
  - **Nguyên tắc**: Service chỉ khai báo queue mà nó CONSUME

- ❌ **Khai báo sai**: Khai báo `notificationExchange` và `notificationQueue`
  - Coupon-service chỉ **PUBLISH** vào queue này, không **CONSUME**  
  - Queue này được CONSUME bởi notification-service
  - Violation của separation of concerns

- ❌ **Thiếu Dead Letter Queue (DLQ)**: Không có DLQ cho error handling
  - Khi xử lý message thất bại, message sẽ mất
  - Không có cơ chế retry và manual intervention

#### Sửa đổi:
✅ **Giữ lại**:
- `orderExchange/orderQueue/orderBinding` - Coupon-service CONSUMES from order.queue
- `redeemResultExchange/redeemResultQueue/redeemResultBinding` - Coupon-service CONSUMES from redeem.result.queue

✅ **Xóa bỏ**:
- `redeemCommandExchange/Queue/Binding` - Moved to order-service (wallet mock consumer)
- `notificationExchange/Queue/Binding` - Already in notification-service (correct)

✅ **Thêm mới**:
- `orderDlqExchange/orderDlqQueue/orderDlqBinding` - Dead letter queue for order processing errors
- `redeemResultDlqExchange/redeemResultDlqQueue/redeemResultDlqBinding` - Dead letter queue for redeem result errors

---

### 2. **Order-Service RabbitMQConfig** - LỖI NGHIÊM TRỌNG
**File:** `order-service/src/main/java/dmd/prj/orderservice/config/RabbitMQConfig.java`

#### Các lỗi:
- ❌ **Khai báo sai**: Khai báo `orderExchange` và `orderQueue`
  - Order-service chỉ **PUBLISH** vào queue này, không **CONSUME**
  - Queue này được CONSUME bởi coupon-service
  
- ❌ **Khai báo sai**: Khai báo `redeemResultExchange` và `redeemResultQueue`
  - Order-service (wallet mock) chỉ **PUBLISH** vào queue này
  - Queue này được CONSUME bởi coupon-service
  
- ❌ **Thiếu queue**: Không khai báo `redeemCommandQueue`
  - Wallet Mock (trong order-service) phải CONSUME queue này
  - Đây là queue duy nhất mà order-service cần khai báo!

- ❌ **Retry queue không cần thiết**: Có `orderRetryExchange` và `orderRetryQueue`
  - Retry logic nên ở consumer side (coupon-service), không phải producer side

#### Sửa đổi:
✅ **Xóa bỏ**:
- `orderExchange/Queue/Binding` - Moved to coupon-service (correct consumer)
- `orderRetryExchange/Queue/Binding` - Retry handled by consumer
- `redeemResultExchange/Queue/Binding` - Moved to coupon-service (correct consumer)

✅ **Thêm mới**:
- `redeemCommandExchange/redeemCommandQueue/redeemCommandBinding` - Wallet Mock CONSUMES this
- `redeemCommandDlqExchange/redeemCommandDlqQueue/redeemCommandDlqBinding` - DLQ for redeem commands

---

### 3. **Notification-Service RabbitMQConfig** - Thiếu DLQ
**File:** `notification-service/src/main/java/dmd/prj/notificationservice/config/RabbitMQConfig.java`

#### Vấn đề:
- ⚠️ **Thiếu Dead Letter Queue**: Không có DLQ cho notification processing errors

#### Sửa đổi:
✅ **Giữ lại**:
- `notificationExchange/Queue/Binding` - Notification-service CONSUMES this (correct)

✅ **Thêm mới**:
- `notificationDlqExchange/notificationDlqQueue/notificationDlqBinding` - DLQ for notification errors

---

## 📋 Kiến trúc RabbitMQ đúng

### Queue Ownership Table

| Queue | Producer | Consumer | Declared In | Notes |
|-------|----------|----------|-------------|-------|
| `order.queue` | order-service | **coupon-service** | coupon-service | ✅ |
| `redeem.command.queue` | coupon-service | **order-service** (wallet mock) | order-service | ✅ |
| `redeem.result.queue` | order-service (wallet mock) | **coupon-service** | coupon-service | ✅ |
| `notification.queue` | coupon-service | **notification-service** | notification-service | ✅ |

**Nguyên tắc vàng**: Service CONSUMER khai báo queue configuration, không phải producer.

### Flow hoàn chỉnh

```
1. ORDER PLACEMENT
   Order Service --[ORDER_PLACED]--> order.exchange --> order.queue
                                                          ↓
                                                    Coupon Service (consumer)

2. REDEEM COMMAND  
   Coupon Service --[REDEEM_COMMAND]--> redeem.command.exchange --> redeem.command.queue
                                                                       ↓
                                                              Order Service / Wallet Mock (consumer)

3. REDEEM RESULT
   Wallet Mock --[REDEEM_RESULT]--> redeem.result.exchange --> redeem.result.queue
                                                                  ↓
                                                            Coupon Service (consumer)

4. NOTIFICATION
   Coupon Service --[NOTIFICATION]--> notification.exchange --> notification.queue
                                                                   ↓
                                                         Notification Service (consumer)
```

### Dead Letter Queue Strategy

Mỗi main queue đều có DLQ tương ứng:
- `order.queue` → `order.dlq.queue`
- `redeem.command.queue` → `redeem.command.dlq.queue`
- `redeem.result.queue` → `redeem.result.dlq.queue`
- `notification.queue` → `notification.dlq.queue`

Khi message xử lý thất bại (sau nhiều retry attempt), message sẽ được route vào DLQ để manual intervention.

---

## 🔧 Dependency Issue (Phát hiện thêm)

### Vấn đề trong Parent POM
**File:** `pom.xml` (root)

#### Lỗi 1: Spring Boot Maven Plugin không nên ở parent pom
```xml
<!-- Lines 89-100: SHOULD BE REMOVED FROM PARENT -->
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    ...
</plugin>
```
- Parent project là `pom` packaging, không cần repackage
- Plugin này chỉ cần trong individual service modules

#### Lỗi 2: Dependency version sai
```xml
<!-- Line 64: WRONG VERSION -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
    <version>${project.parent.version}</version>  <!-- ❌ This is 1.0.0-SNAPSHOT! -->
</dependency>

<!-- Line 70: WRONG VERSION -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
    <version>${project.parent.version}</version>  <!-- ❌ This is 1.0.0-SNAPSHOT! -->
</dependency>
```

**Fix:**
```xml
<!-- Should not specify version - inherit from spring-boot-starter-parent -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

---

## ✅ Tổng kết

### Đã sửa:
1. ✅ **coupon-service/RabbitMQConfig** - Removed wrong queue declarations, added DLQs
2. ✅ **order-service/RabbitMQConfig** - Removed wrong queue declarations, added redeem-command queue + DLQ
3. ✅ **notification-service/RabbitMQConfig** - Added DLQ

### Cần sửa thêm (trong parent pom.xml):
1. ⚠️ Remove spring-boot-maven-plugin from parent pom (lines 89-100)
2. ⚠️ Fix dependency versions for redis and amqp (lines 63-71)

### Lưu ý:
- IntelliJ có thể vẫn hiển thị "Cannot resolve symbol 'amqp'" cho đến khi Maven sync thành công
- Cần Java 17+ để build project (hiện đang dùng Java 8)
- Sau khi sync Maven, các lỗi compilation sẽ biến mất

---

**Generated:** 2026-06-29  
**Author:** GitHub Copilot  
**Status:** ✅ Code fixes completed, awaiting Maven sync

