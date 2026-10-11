package org.liangyou.modules.basic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WarehouseSaveRequest {

    @NotBlank(message = "warehouse-code-required")
    private String warehouseCode;
    @NotBlank(message = "warehouse-name-required")
    private String warehouseName;
    private String contactName;
    private String contactPhone;
    private String address;
    private Integer status;
    private String remark;
}
