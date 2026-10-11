package org.liangyou.modules.basic.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UnitConvertSaveRequest {

    @NotNull(message = "product-id-required")
    private Long productId;
    @NotNull(message = "from-unit-id-required")
    private Long fromUnitId;
    @NotNull(message = "to-unit-id-required")
    private Long toUnitId;
    @NotNull(message = "convert-rate-required")
    @DecimalMin(value = "0.000001", message = "convert-rate-invalid")
    private BigDecimal convertRate;
    private Integer status;
    private String remark;
}
