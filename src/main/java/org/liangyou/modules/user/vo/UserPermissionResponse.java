package org.liangyou.modules.user.vo;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "用户权限响应")
public class UserPermissionResponse {

    @Schema(description = "用户 ID", example = "100")
    private Long userId;
    @ArraySchema(schema = @Schema(description = "角色 ID", example = "1"))
    private List<Long> roleIds;
    @ArraySchema(schema = @Schema(description = "权限 ID", example = "101"))
    private List<Long> permissionIds;
}
