package dmd.prj.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedeemResultEvent {
    private String txnId;
    private Boolean success;
    private String errorCode;
}
