package org.liangyou.modules.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "库存修正请求")
public class InventoryUpdateRequest {

    @NotNull(message = "total-qty-required")
    @Schema(description = "总库存数量", example = "520")
    private BigDecimal totalQty;
    @NotNull(message = "available-qty-required")
    @Schema(description = "可用库存数量", example = "500")
    private BigDecimal availableQty;
    @Schema(description = "修正备注", example = "盘点修正")
    private String remark;
}
