package org.liangyou.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "新增角色请求")
public class RoleCreateRequest {

    @NotBlank(message = "role-code-required")
    @Schema(description = "角色编码", example = "WAREHOUSE")
    private String roleCode;

    @NotBlank(message = "role-name-required")
    @Schema(description = "角色名称", example = "仓库人员")
    private String roleName;

    @Schema(description = "状态，1 启用 0 停用，默认 1", example = "1")
    private Integer status;

    @Schema(description = "备注", example = "仓库角色")
    private String remark;
}
