package org.liangyou.modules.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "销售出库查询参数")
public class SaleOutQueryRequest {

    @Schema(description = "页码", example = "1")
    private long pageNum = 1;
    @Schema(description = "每页条数", example = "10")
    private long pageSize = 10;
    @Schema(description = "出库单号", example = "SO20260416123000001")
    private String orderNo;
    @Schema(description = "仓库 ID", example = "1")
    private Long warehouseId;
    @Schema(description = "客户 ID", example = "20")
    private Long customerId;
    @Schema(description = "状态", example = "1")
    private Integer status;
    @Schema(description = "业务开始日期", example = "2026-04-01")
    private LocalDate bizDateFrom;
    @Schema(description = "业务结束日期", example = "2026-04-30")
    private LocalDate bizDateTo;
}
