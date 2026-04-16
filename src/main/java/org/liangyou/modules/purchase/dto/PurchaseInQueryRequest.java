package org.liangyou.modules.purchase.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PurchaseInQueryRequest {

    private long pageNum = 1;
    private long pageSize = 10;
    private String orderNo;
    private Long warehouseId;
    private Long supplierId;
    private Integer status;
    private LocalDate bizDateFrom;
    private LocalDate bizDateTo;
}
