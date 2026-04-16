package org.liangyou.modules.statistics.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StatisticsQueryRequest {

    private Long warehouseId;
    private String statType;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String monthFrom;
    private String monthTo;
    private Long customerId;
    private Long supplierId;
}
