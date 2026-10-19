package org.liangyou.modules.basic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Data
public class CategorySaveRequest {

    @NotBlank(message = "category-name-required")
    @Size(max = 128, message = "categoryName-too-long")
    private String categoryName;
    @NotNull(message = "parent-id-required")
    @Min(value = 0, message = "category-parent-invalid")
    private Long parentId;
    private Integer sortNo;
    @Min(value = 0, message = "status-invalid")
    @Max(value = 1, message = "status-invalid")
    private Integer status;
    @Size(max = 255, message = "remark-too-long")
    private String remark;
}
