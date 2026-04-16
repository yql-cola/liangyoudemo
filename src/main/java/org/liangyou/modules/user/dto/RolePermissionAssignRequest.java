package org.liangyou.modules.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RolePermissionAssignRequest {

    @NotNull(message = "permission-ids-required")
    private List<Long> permissionIds;
}
