package org.liangyou.modules.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "销售出库新增请求")
public class SaleOutCreateRequest {

    @NotNull(message = "warehouse-id-required")
    @Schema(description = "仓库 ID", example = "1")
    private Long warehouseId;
    @Schema(description = "客户 ID", example = "20")
    private Long customerId;
    private String customerName;
    private String saleType = "retail";
    private String fulfillmentType = "pickup";
    private String contactName;
    private String contactPhone;
    private String deliveryAddress;
    @Size(max = 255, message = "delivery-remark-too-long")
    private String deliveryRemark;
    private String handlerName;
    private java.time.LocalDateTime plannedDeliveryTime;
    @NotNull(message = "biz-date-required")
    @Schema(description = "业务日期", example = "2026-04-16")
    private LocalDate bizDate;
    @Schema(description = "备注", example = "日常销售")
    private String remark;
    @Valid
    @NotEmpty(message = "items-required")
    @Schema(description = "销售明细")
    private List<SaleOutItemRequest> items;
}
