package org.liangyou.modules.basic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerSaveRequest {

    @NotBlank(message = "customer-code-required")
    private String customerCode;
    @NotBlank(message = "customer-name-required")
    private String customerName;
    private String contactName;
    private String contactPhone;
    private String address;
    private String taxNo;
    private String bankName;
    private String bankAccount;
    private Integer status;
    private String remark;
}
