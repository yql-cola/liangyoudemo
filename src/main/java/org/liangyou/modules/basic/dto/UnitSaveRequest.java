package org.liangyou.modules.basic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Data
public class UnitSaveRequest {

    @NotBlank(message = "unit-code-required")
    @Size(max = 32, message = "unitCode-too-long")
    private String unitCode;
    @NotBlank(message = "unit-name-required")
    @Size(max = 32, message = "unitName-too-long")
    private String unitName;
    @Min(value = 0, message = "status-invalid")
    @Max(value = 1, message = "status-invalid")
    private Integer status;
    @Size(max = 255, message = "remark-too-long")
    private String remark;
}
