package dmd.prj.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String dedupKey;
    private String userId;
    private String type;  // REDEEM_SUCCESS, REDEEM_FAILED, ADMIN_ALERT
    private String title;
    private String message;
    private Boolean adminAlert;
}
