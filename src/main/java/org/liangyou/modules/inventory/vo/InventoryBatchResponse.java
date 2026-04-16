package org.liangyou.modules.inventory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "库存批次响应")
public class InventoryBatchResponse {

    @Schema(description = "批次 ID", example = "1")
    private Long id;
    @Schema(description = "仓库 ID", example = "1")
    private Long warehouseId;
    @Schema(description = "商品 ID", example = "1001")
    private Long productId;
    @Schema(description = "批次号", example = "B20260416001")
    private String batchNo;
    @Schema(description = "入库时间", example = "2026-04-16T10:00:00")
    private LocalDateTime inboundTime;
    @Schema(description = "生产日期", example = "2026-04-01")
    private LocalDate productionDate;
    @Schema(description = "过期日期", example = "2026-10-01")
    private LocalDate expiryDate;
    @Schema(description = "单价", example = "120.50")
    private BigDecimal unitPrice;
    @Schema(description = "初始数量", example = "100")
    private BigDecimal initQty;
    @Schema(description = "剩余数量", example = "80")
    private BigDecimal remainQty;
}
