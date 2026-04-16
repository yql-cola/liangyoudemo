package org.liangyou.modules.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserPermissionAssignRequest {

    private List<Long> grantPermissionIds;
    private List<Long> revokePermissionIds;
}
