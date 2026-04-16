package org.liangyou.modules.inventory.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InventoryFlowQueryRequest {

    private Long warehouseId;
    private Long productId;
    private LocalDateTime bizTimeFrom;
    private LocalDateTime bizTimeTo;
}
