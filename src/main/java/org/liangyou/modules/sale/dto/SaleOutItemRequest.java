package org.liangyou.modules.sale.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaleOutItemRequest {

    @NotNull(message = "product-id-required")
    private Long productId;
    @NotNull(message = "unit-id-required")
    private Long unitId;
    @NotNull(message = "qty-required")
    private BigDecimal qty;
    @NotNull(message = "unit-price-required")
    private BigDecimal unitPrice;
    @NotNull(message = "amount-required")
    private BigDecimal amount;
    private String remark;
}
