package org.liangyou.modules.basic.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.math.BigDecimal;

@Data
public class UnitConvertSaveRequest {

    @NotNull(message = "product-id-required")
    @Positive(message = "productId-invalid")
    private Long productId;
    @NotNull(message = "from-unit-id-required")
    @Positive(message = "fromUnitId-invalid")
    private Long fromUnitId;
    @NotNull(message = "to-unit-id-required")
    @Positive(message = "toUnitId-invalid")
    private Long toUnitId;
    @NotNull(message = "convert-rate-required")
    @DecimalMin(value = "0.000001", message = "convert-rate-invalid")
    @Digits(integer = 12, fraction = 6, message = "convert-rate-invalid")
    private BigDecimal convertRate;
    @Min(value = 0, message = "status-invalid")
    @Max(value = 1, message = "status-invalid")
    private Integer status;
    @Size(max = 255, message = "remark-too-long")
    private String remark;
}
