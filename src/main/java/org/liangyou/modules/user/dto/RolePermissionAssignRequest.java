package org.liangyou.modules.user.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "角色权限分配请求")
public class RolePermissionAssignRequest {

    @NotNull(message = "permission-ids-required")
    @ArraySchema(schema = @Schema(description = "权限 ID", example = "101"))
    private List<Long> permissionIds;
}
