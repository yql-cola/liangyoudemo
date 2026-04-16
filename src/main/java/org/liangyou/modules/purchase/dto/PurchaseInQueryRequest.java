package org.liangyou.modules.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "采购入库查询参数")
public class PurchaseInQueryRequest {

    @Schema(description = "页码", example = "1")
    private long pageNum = 1;
    @Schema(description = "每页条数", example = "10")
    private long pageSize = 10;
    @Schema(description = "入库单号", example = "PI20260416123000001")
    private String orderNo;
    @Schema(description = "仓库 ID", example = "1")
    private Long warehouseId;
    @Schema(description = "供应商 ID", example = "10")
    private Long supplierId;
    @Schema(description = "状态", example = "1")
    private Integer status;
    @Schema(description = "业务开始日期", example = "2026-04-01")
    private LocalDate bizDateFrom;
    @Schema(description = "业务结束日期", example = "2026-04-30")
    private LocalDate bizDateTo;
}
