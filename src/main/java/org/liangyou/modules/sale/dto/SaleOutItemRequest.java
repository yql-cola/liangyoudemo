package org.liangyou.modules.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "销售出库明细")
public class SaleOutItemRequest {

    @NotNull(message = "product-id-required")
    @Schema(description = "商品 ID", example = "1001")
    private Long productId;
    @NotNull(message = "unit-id-required")
    @Schema(description = "单位 ID", example = "1")
    private Long unitId;
    @NotNull(message = "qty-required")
    @Schema(description = "数量", example = "20")
    private BigDecimal qty;
    @NotNull(message = "unit-price-required")
    @Schema(description = "销售单价", example = "150.00")
    private BigDecimal unitPrice;
    @NotNull(message = "amount-required")
    @Schema(description = "金额", example = "3000.00")
    private BigDecimal amount;
    @Schema(description = "备注", example = "门店零售")
    private String remark;
}
