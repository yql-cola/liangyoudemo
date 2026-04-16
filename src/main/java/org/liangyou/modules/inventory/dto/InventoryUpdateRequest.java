package org.liangyou.modules.inventory.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryUpdateRequest {

    @NotNull(message = "total-qty-required")
    private BigDecimal totalQty;
    @NotNull(message = "available-qty-required")
    private BigDecimal availableQty;
    private String remark;
}
