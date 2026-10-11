package org.liangyou.modules.basic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductSaveRequest {

    @NotBlank(message = "product-code-required")
    private String productCode;
    @NotBlank(message = "product-name-required")
    private String productName;
    @NotNull(message = "category-id-required")
    private Long categoryId;
    private String brandName;
    private String spec;
    private String barcode;
    private String imageUrl;
    @NotNull(message = "unit-id-required")
    private Long baseUnitId;
    private Integer shelfLifeDays;
    private Integer status;
    private String remark;
}
