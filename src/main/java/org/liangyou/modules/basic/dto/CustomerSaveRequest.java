package org.liangyou.modules.basic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Data
public class CustomerSaveRequest {

    @NotBlank(message = "customer-code-required")
    @Size(max = 64, message = "customerCode-too-long")
    private String customerCode;
    @NotBlank(message = "customer-name-required")
    @Size(max = 128, message = "customerName-too-long")
    private String customerName;
    @Size(max = 64, message = "contactName-too-long")
    private String contactName;
    @Size(max = 32, message = "contactPhone-too-long")
    private String contactPhone;
    @Size(max = 255, message = "address-too-long")
    private String address;
    @Size(max = 64, message = "taxNo-too-long")
    private String taxNo;
    @Size(max = 128, message = "bankName-too-long")
    private String bankName;
    @Size(max = 64, message = "bankAccount-too-long")
    private String bankAccount;
    @Min(value = 0, message = "status-invalid")
    @Max(value = 1, message = "status-invalid")
    private Integer status;
    @Size(max = 255, message = "remark-too-long")
    private String remark;
}
