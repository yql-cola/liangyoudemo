package org.liangyou.modules.sale.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SaleOutQueryRequest {

    private long pageNum = 1;
    private long pageSize = 10;
    private String orderNo;
    private Long warehouseId;
    private Long customerId;
    private Integer status;
    private LocalDate bizDateFrom;
    private LocalDate bizDateTo;
}
