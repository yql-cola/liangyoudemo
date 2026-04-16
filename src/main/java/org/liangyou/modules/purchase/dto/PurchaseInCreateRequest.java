package org.liangyou.modules.purchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "采购入库新增请求")
public class PurchaseInCreateRequest {

    @NotNull(message = "warehouse-id-required")
    @Schema(description = "仓库 ID", example = "1")
    private Long warehouseId;
    @NotNull(message = "supplier-id-required")
    @Schema(description = "供应商 ID", example = "10")
    private Long supplierId;
    @NotNull(message = "biz-date-required")
    @Schema(description = "业务日期", example = "2026-04-16")
    private LocalDate bizDate;
    @Schema(description = "状态", example = "1")
    private Integer status = 1;
    @Schema(description = "备注", example = "首批采购")
    private String remark;
    @Valid
    @NotEmpty(message = "items-required")
    @Schema(description = "采购明细")
    private List<PurchaseInItemRequest> items;
}
