package org.liangyou.modules.statistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "统计查询参数")
public class StatisticsQueryRequest {

    @Schema(description = "仓库 ID", example = "1")
    private Long warehouseId;
    @Schema(description = "统计类型", example = "sale")
    private String statType;
    @Schema(description = "开始日期", example = "2026-04-01")
    private LocalDate dateFrom;
    @Schema(description = "结束日期", example = "2026-04-30")
    private LocalDate dateTo;
    @Schema(description = "开始月份", example = "2026-04")
    private String monthFrom;
    @Schema(description = "结束月份", example = "2026-06")
    private String monthTo;
    @Schema(description = "客户 ID", example = "20")
    private Long customerId;
    @Schema(description = "供应商 ID", example = "10")
    private Long supplierId;
}
