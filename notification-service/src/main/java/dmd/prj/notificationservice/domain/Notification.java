package dmd.prj.notificationservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_user", columnList = "user_id,status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "dedup_key", unique = true, nullable = false, length = 160)
    private String dedupKey;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @Column(name = "title", nullable = false, length = 256)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "UNREAD";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
