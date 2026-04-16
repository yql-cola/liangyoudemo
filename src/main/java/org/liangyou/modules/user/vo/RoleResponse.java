package org.liangyou.modules.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "角色响应")
public class RoleResponse {

    @Schema(description = "角色 ID", example = "1")
    private Long id;

    @Schema(description = "角色编码", example = "WAREHOUSE")
    private String roleCode;

    @Schema(description = "角色名称", example = "仓库人员")
    private String roleName;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "备注", example = "仓库角色")
    private String remark;
}
