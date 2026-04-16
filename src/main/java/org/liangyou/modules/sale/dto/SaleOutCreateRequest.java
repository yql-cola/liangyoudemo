package org.liangyou.modules.sale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SaleOutCreateRequest {

    @NotNull(message = "warehouse-id-required")
    private Long warehouseId;
    @NotNull(message = "customer-id-required")
    private Long customerId;
    @NotNull(message = "biz-date-required")
    private LocalDate bizDate;
    private Integer status = 1;
    private String remark;
    @Valid
    @NotEmpty(message = "items-required")
    private List<SaleOutItemRequest> items;
}
