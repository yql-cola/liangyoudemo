package org.liangyou.modules.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "库存汇总查询参数")
public class InventoryQueryRequest {

    @Schema(description = "页码", example = "1")
    private long pageNum = 1;
    @Schema(description = "每页条数", example = "10")
    private long pageSize = 10;
    @Schema(description = "仓库 ID", example = "1")
    private Long warehouseId;
    @Schema(description = "商品 ID", example = "1001")
    private Long productId;
}
