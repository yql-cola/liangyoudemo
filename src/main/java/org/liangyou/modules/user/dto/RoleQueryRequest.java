package org.liangyou.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "角色查询参数")
public class RoleQueryRequest {

    @Schema(description = "角色名称", example = "仓库")
    private String roleName;

    @Schema(description = "角色编码", example = "WAREHOUSE")
    private String roleCode;

    @Schema(description = "状态", example = "1")
    private Integer status;
}
