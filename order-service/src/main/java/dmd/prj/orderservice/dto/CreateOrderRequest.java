package dmd.prj.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    private String productType;
    private BigDecimal amount;
    private String market;
}
