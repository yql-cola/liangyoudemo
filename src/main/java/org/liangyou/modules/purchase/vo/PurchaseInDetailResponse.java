package org.liangyou.modules.purchase.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseInDetailResponse {

    private Long id;
    private String orderNo;
    private Long warehouseId;
    private Long supplierId;
    private LocalDate bizDate;
    private BigDecimal totalAmount;
    private Integer status;
    private String remark;
    private List<Item> items;

    @Data
    public static class Item {
        private Long id;
        private Long productId;
        private Long unitId;
        private BigDecimal qty;
        private BigDecimal unitPrice;
        private BigDecimal amount;
        private String batchNo;
        private LocalDate productionDate;
        private LocalDate expiryDate;
        private LocalDateTime inboundTime;
        private String remark;
    }
}
