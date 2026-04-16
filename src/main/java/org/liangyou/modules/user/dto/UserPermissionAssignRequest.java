package org.liangyou.modules.user.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "用户单独授权请求")
public class UserPermissionAssignRequest {

    @ArraySchema(schema = @Schema(description = "直接授予的权限 ID", example = "201"))
    private List<Long> grantPermissionIds;
    @ArraySchema(schema = @Schema(description = "直接收回的权限 ID", example = "202"))
    private List<Long> revokePermissionIds;
}
