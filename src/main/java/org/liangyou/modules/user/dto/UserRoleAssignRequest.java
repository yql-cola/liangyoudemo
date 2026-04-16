package org.liangyou.modules.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UserRoleAssignRequest {

    @NotNull(message = "role-ids-required")
    private List<Long> roleIds;
}
