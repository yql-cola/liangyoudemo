package org.liangyou.modules.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "销售出库新增请求")
public class SaleOutCreateRequest {

    @NotNull(message = "warehouse-id-required")
    @Schema(description = "仓库 ID", example = "1")
    private Long warehouseId;
    @NotNull(message = "customer-id-required")
    @Schema(description = "客户 ID", example = "20")
    private Long customerId;
    @NotNull(message = "biz-date-required")
    @Schema(description = "业务日期", example = "2026-04-16")
    private LocalDate bizDate;
    @Schema(description = "状态", example = "1")
    private Integer status = 1;
    @Schema(description = "备注", example = "日常销售")
    private String remark;
    @Valid
    @NotEmpty(message = "items-required")
    @Schema(description = "销售明细")
    private List<SaleOutItemRequest> items;
}
