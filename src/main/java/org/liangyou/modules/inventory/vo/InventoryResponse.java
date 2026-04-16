package org.liangyou.modules.inventory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "库存汇总响应")
public class InventoryResponse {

    @Schema(description = "库存记录 ID", example = "1")
    private Long id;
    @Schema(description = "仓库 ID", example = "1")
    private Long warehouseId;
    @Schema(description = "商品 ID", example = "1001")
    private Long productId;
    @Schema(description = "总数量", example = "520")
    private BigDecimal totalQty;
    @Schema(description = "锁定数量", example = "20")
    private BigDecimal lockedQty;
    @Schema(description = "可用数量", example = "500")
    private BigDecimal availableQty;
}
