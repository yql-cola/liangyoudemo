package org.liangyou.modules.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "采购入库明细")
public class PurchaseInItemRequest {

    @NotNull(message = "product-id-required")
    @Schema(description = "商品 ID", example = "1001")
    private Long productId;
    @NotNull(message = "unit-id-required")
    @Schema(description = "单位 ID", example = "1")
    private Long unitId;
    @NotNull(message = "qty-required")
    @Schema(description = "数量", example = "100")
    private BigDecimal qty;
    @NotNull(message = "unit-price-required")
    @Schema(description = "单价", example = "120.50")
    private BigDecimal unitPrice;
    @NotNull(message = "amount-required")
    @Schema(description = "金额", example = "12050.00")
    private BigDecimal amount;
    @NotBlank(message = "batch-no-required")
    @Schema(description = "批次号", example = "B20260416001")
    private String batchNo;
    @Schema(description = "生产日期", example = "2026-04-01")
    private LocalDate productionDate;
    @Schema(description = "过期日期", example = "2026-10-01")
    private LocalDate expiryDate;
    @NotNull(message = "inbound-time-required")
    @Schema(description = "入库时间", example = "2026-04-16T10:00:00")
    private LocalDateTime inboundTime;
    @Schema(description = "备注", example = "整箱入库")
    private String remark;
}
