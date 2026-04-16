package org.liangyou.modules.user.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "用户角色分配请求")
public class UserRoleAssignRequest {

    @NotNull(message = "role-ids-required")
    @ArraySchema(schema = @Schema(description = "角色 ID", example = "1"))
    private List<Long> roleIds;
}
