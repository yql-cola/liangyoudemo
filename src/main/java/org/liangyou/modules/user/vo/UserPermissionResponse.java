package org.liangyou.modules.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserPermissionResponse {

    private Long userId;
    private List<Long> roleIds;
    private List<Long> permissionIds;
}
