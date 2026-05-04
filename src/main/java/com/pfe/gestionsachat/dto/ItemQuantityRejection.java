package com.pfe.gestionsachat.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ItemQuantityRejection {
    private Long stockItemId;
    private Integer quantity;
    private BigDecimal unitPrice;
}
