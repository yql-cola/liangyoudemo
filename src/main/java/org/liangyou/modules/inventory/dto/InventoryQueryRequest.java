package org.liangyou.modules.inventory.dto;

import lombok.Data;

@Data
public class InventoryQueryRequest {

    private long pageNum = 1;
    private long pageSize = 10;
    private Long warehouseId;
    private Long productId;
}
