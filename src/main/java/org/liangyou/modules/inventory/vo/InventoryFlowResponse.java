package org.liangyou.modules.inventory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "库存流水响应")
public class InventoryFlowResponse {

    @Schema(description = "流水 ID", example = "1")
    private Long id;
    @Schema(description = "仓库 ID", example = "1")
    private Long warehouseId;
    @Schema(description = "商品 ID", example = "1001")
    private Long productId;
    @Schema(description = "批次 ID", example = "10")
    private Long batchId;
    @Schema(description = "流水类型", example = "purchase_in")
    private String flowType;
    @Schema(description = "业务类型", example = "purchase_in")
    private String bizType;
    @Schema(description = "业务单据 ID", example = "200")
    private Long bizId;
    @Schema(description = "业务明细 ID", example = "201")
    private Long bizItemId;
    @Schema(description = "变动数量", example = "100")
    private BigDecimal changeQty;
    @Schema(description = "变动后数量", example = "520")
    private BigDecimal afterQty;
    @Schema(description = "业务时间", example = "2026-04-16T10:00:00")
    private LocalDateTime bizTime;
    @Schema(description = "备注", example = "采购入库增加库存")
    private String remark;
}
