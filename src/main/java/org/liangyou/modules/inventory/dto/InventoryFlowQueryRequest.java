package org.liangyou.modules.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "库存流水查询参数")
public class InventoryFlowQueryRequest {

    @Schema(description = "仓库 ID", example = "1")
    private Long warehouseId;
    @Schema(description = "商品 ID", example = "1001")
    private Long productId;
    @Schema(description = "业务开始时间", example = "2026-04-01T00:00:00")
    private LocalDateTime bizTimeFrom;
    @Schema(description = "业务结束时间", example = "2026-04-30T23:59:59")
    private LocalDateTime bizTimeTo;
}
