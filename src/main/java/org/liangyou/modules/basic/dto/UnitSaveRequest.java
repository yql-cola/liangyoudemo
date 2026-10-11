package org.liangyou.modules.basic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UnitSaveRequest {

    @NotBlank(message = "unit-code-required")
    private String unitCode;
    @NotBlank(message = "unit-name-required")
    private String unitName;
    private Integer status;
    private String remark;
}
