package dmd.prj.orderservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "txn_id", unique = true, nullable = false, length = 128)
    private String txnId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "product_type", nullable = false, length = 16)
    private String productType;  // STOCK, FX, ETF

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "market", nullable = false, length = 32)
    private String market;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
