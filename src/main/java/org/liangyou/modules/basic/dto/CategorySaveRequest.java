package org.liangyou.modules.basic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategorySaveRequest {

    @NotBlank(message = "category-name-required")
    private String categoryName;
    @NotNull(message = "parent-id-required")
    private Long parentId;
    private Integer sortNo;
    private Integer status;
    private String remark;
}
