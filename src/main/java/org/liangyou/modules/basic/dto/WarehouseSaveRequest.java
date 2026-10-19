package org.liangyou.modules.basic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Data
public class WarehouseSaveRequest {

    @NotBlank(message = "warehouse-code-required")
    @Size(max = 64, message = "warehouseCode-too-long")
    private String warehouseCode;
    @NotBlank(message = "warehouse-name-required")
    @Size(max = 128, message = "warehouseName-too-long")
    private String warehouseName;
    @Size(max = 64, message = "contactName-too-long")
    private String contactName;
    @Size(max = 32, message = "contactPhone-too-long")
    private String contactPhone;
    @Size(max = 255, message = "address-too-long")
    private String address;
    @Min(value = 0, message = "status-invalid")
    @Max(value = 1, message = "status-invalid")
    private Integer status;
    @Size(max = 255, message = "remark-too-long")
    private String remark;
}
