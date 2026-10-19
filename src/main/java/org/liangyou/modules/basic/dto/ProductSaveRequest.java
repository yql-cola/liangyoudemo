package org.liangyou.modules.basic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Data
public class ProductSaveRequest {

    @NotBlank(message = "product-code-required")
    @Size(max = 64, message = "productCode-too-long")
    private String productCode;
    @NotBlank(message = "product-name-required")
    @Size(max = 128, message = "productName-too-long")
    private String productName;
    @NotNull(message = "category-id-required")
    @Positive(message = "categoryId-invalid")
    private Long categoryId;
    @Size(max = 64, message = "brandName-too-long")
    private String brandName;
    @Size(max = 128, message = "spec-too-long")
    private String spec;
    @Size(max = 64, message = "barcode-too-long")
    private String barcode;
    @Size(max = 255, message = "imageUrl-too-long")
    private String imageUrl;
    @NotNull(message = "unit-id-required")
    @Positive(message = "baseUnitId-invalid")
    private Long baseUnitId;
    @Min(value = 0, message = "shelf-life-invalid")
    private Integer shelfLifeDays;
    @Min(value = 0, message = "status-invalid")
    @Max(value = 1, message = "status-invalid")
    private Integer status;
    @Size(max = 255, message = "remark-too-long")
    private String remark;
}
