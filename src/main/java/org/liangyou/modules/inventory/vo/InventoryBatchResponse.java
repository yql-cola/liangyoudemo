package org.liangyou.modules.inventory.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InventoryBatchResponse {

    private Long id;
    private Long warehouseId;
    private Long productId;
    private String batchNo;
    private LocalDateTime inboundTime;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    private BigDecimal unitPrice;
    private BigDecimal initQty;
    private BigDecimal remainQty;
}
