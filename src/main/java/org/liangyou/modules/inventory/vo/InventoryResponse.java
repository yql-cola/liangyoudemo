package org.liangyou.modules.inventory.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryResponse {

    private Long id;
    private Long warehouseId;
    private Long productId;
    private BigDecimal totalQty;
    private BigDecimal lockedQty;
    private BigDecimal availableQty;
}
