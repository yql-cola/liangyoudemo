package org.liangyou.modules.inventory.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InventoryFlowResponse {

    private Long id;
    private Long warehouseId;
    private Long productId;
    private Long batchId;
    private String flowType;
    private String bizType;
    private Long bizId;
    private Long bizItemId;
    private BigDecimal changeQty;
    private BigDecimal afterQty;
    private LocalDateTime bizTime;
    private String remark;
}
