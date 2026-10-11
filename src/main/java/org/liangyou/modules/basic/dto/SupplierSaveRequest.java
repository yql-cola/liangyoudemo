package org.liangyou.modules.basic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupplierSaveRequest {

    @NotBlank(message = "supplier-code-required")
    private String supplierCode;
    @NotBlank(message = "supplier-name-required")
    private String supplierName;
    private String contactName;
    private String contactPhone;
    private String address;
    private String taxNo;
    private String bankName;
    private String bankAccount;
    private Integer status;
    private String remark;
}
