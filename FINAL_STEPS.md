# 🎯 BƯỚC CUỐI CÙNG - Set JAVA_HOME & IntelliJ Sync

## ✅ Hiện trạng

- ✅ **Java 21 đã cài đặt**: `C:\Program Files\Java\jdk-21\bin\java.exe`
- ✅ **Build thành công**: 9/9 modules compiled successfully
- ✅ **Code đã sửa**: Tất cả RabbitMQConfig files đã được fix

---

## 🔧 Set JAVA_HOME vĩnh viễn (Khuyến nghị)

### Option 1: PowerShell (Admin) - Nhanh nhất

```powershell
# Mở PowerShell as Administrator
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-21", "Machine")
[System.Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\Program Files\Java\jdk-21\bin", "Machine")
```

### Option 2: System Properties GUI

1. Nhấn `Win + X` → chọn **System**
2. Click **Advanced system settings**
3. Click **Environment Variables**
4. Trong **System variables**:
   - Click **New**
   - Variable name: `JAVA_HOME`
   - Variable value: `C:\Program Files\Java\jdk-21`
   - Click **OK**
5. Tìm biến `Path` → Click **Edit** → **New**
   - Thêm: `%JAVA_HOME%\bin`
   - Click **OK**

### Verify

```powershell
# Mở PowerShell mới (sau khi set biến môi trường)
echo $env:JAVA_HOME
# Output: C:\Program Files\Java\jdk-21

java -version
# Output: java version "21.0.7" 2025-04-15 LTS

mvn -version
# Output: Apache Maven ... Java version: 21.0.7
```

---

## 🔄 IntelliJ IDEA - Reload Maven Projects

### Bước 1: Mở Maven Tab
- Nhấn `Ctrl + Shift + A` (Find Action)
- Gõ `Maven`
- Chọn **Maven tool window**

### Bước 2: Reload Projects
- Click icon **🔄 Reload All Maven Projects** ở góc trên bên trái Maven tab
- Hoặc: Right-click vào root project → **Maven** → **Reload project**

### Bước 3: Đợi Indexing
- IntelliJ sẽ re-index (~30-60 giây)
- Thanh progress hiển thị ở góc dưới bên phải

### Bước 4: Verify
Sau khi index xong:
- ✅ Các import `org.springframework.amqp.core.*` không còn màu đỏ
- ✅ Các class `DirectExchange`, `Queue`, `Binding` được resolve
- ✅ Không còn lỗi compilation trong RabbitMQConfig files

---

## 📊 Verify All Fixes

### Kiểm tra các file đã sửa:

```powershell
# Trong terminal (PowerShell)
cd D:\dauminhduc_data\prj\demomicroservices

# Verify coupon-service config
Get-Content coupon-service\src\main\java\dmd\prj\couponservice\config\RabbitMQConfig.java | Select-String -Pattern "Queue|Exchange" | Measure-Object
# Kết quả: Phải có 12 lines (6 queues: order, orderDlq, redeemResult, redeemResultDlq + exchanges + bindings)

# Verify order-service config
Get-Content order-service\src\main\java\dmd\prj\orderservice\config\RabbitMQConfig.java | Select-String -Pattern "Queue|Exchange" | Measure-Object
# Kết quả: Phải có 6 lines (3 queues: redeemCommand, redeemCommandDlq + exchanges + bindings)

# Verify notification-service config
Get-Content notification-service\src\main\java\dmd\prj\notificationservice\config\RabbitMQConfig.java | Select-String -Pattern "Queue|Exchange" | Measure-Object
# Kết quả: Phải có 6 lines (3 queues: notification, notificationDlq + exchanges + bindings)
```

### Build lại để chắc chắn:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
cd D:\dauminhduc_data\prj\demomicroservices
mvn clean install -DskipTests
```

Nếu thấy `BUILD SUCCESS` → Everything is good! ✅

---

## 🐛 Troubleshooting

### Issue 1: Maven vẫn dùng Java 8
**Solution:**
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
mvn -version  # Verify Java 21
```

### Issue 2: IntelliJ vẫn hiển thị lỗi sau khi reload
**Solution:**
1. File → Invalidate Caches / Restart
2. Chọn **Invalidate and Restart**
3. Đợi IntelliJ restart và re-index

### Issue 3: Dependencies không download
**Solution:**
```powershell
# Force update dependencies
mvn clean install -U -DskipTests
```

---

## 📋 Tóm tắt các file đã sửa

| File | Changes | Status |
|------|---------|--------|
| `coupon-service/RabbitMQConfig.java` | Removed wrong queues, added DLQs | ✅ Fixed |
| `order-service/RabbitMQConfig.java` | Added redeemCommand queue, removed wrong queues | ✅ Fixed |
| `notification-service/RabbitMQConfig.java` | Added DLQ | ✅ Fixed |
| `pom.xml` (root) | Fixed dependency versions, removed wrong plugin | ✅ Fixed |

---

## 🎓 What You Learned

1. **RabbitMQ Best Practice**: Consumer declares the queue, not producer
2. **Dead Letter Queues**: Essential for error handling in microservices
3. **Maven Dependency Management**: Spring Boot parent manages versions
4. **Separation of Concerns**: Each service owns its consumer configurations

---

## 🚀 Next Steps

Sau khi IntelliJ reload Maven thành công:

1. **Implement Business Logic**:
   - [ ] CouponService matching logic
   - [ ] OrderEventListener (consume order.queue)
   - [ ] RedeemResultListener (consume redeem.result.queue)
   - [ ] WalletMockListener (consume redeem.command.queue)
   - [ ] NotificationEventListener (consume notification.queue)

2. **Test RabbitMQ Configuration**:
   ```bash
   docker-compose up -d rabbitmq
   # Start services và verify queues được tạo đúng
   ```

3. **Integration Testing**:
   - Test complete order → coupon → wallet → notification flow

---

**Ngày:** 2026-06-29  
**Trạng thái:** ✅ Ready for IntelliJ Sync  
**Build:** SUCCESS (19.5s)

