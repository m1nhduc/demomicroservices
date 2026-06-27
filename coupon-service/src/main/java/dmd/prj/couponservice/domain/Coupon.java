package dmd.prj.couponservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons", indexes = {
    @Index(name = "idx_coupons_user_status", columnList = "user_id,status"),
    @Index(name = "idx_coupons_expired_at", columnList = "expired_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "AVAILABLE";

    @Column(name = "reward_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal rewardAmount;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "conditions", columnDefinition = "JSONB")
    private String conditions;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
