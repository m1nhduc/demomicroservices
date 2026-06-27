package dmd.prj.couponservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_tracking", indexes = {
    @Index(name = "idx_tracking_recovery", columnList = "status,updated_at"),
    @Index(name = "idx_tracking_user", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionTracking {
    @Id
    @Column(name = "txn_id", length = 128)
    private String txnId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "INIT";

    @Column(name = "payload", nullable = false, columnDefinition = "JSONB")
    private String payload;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
