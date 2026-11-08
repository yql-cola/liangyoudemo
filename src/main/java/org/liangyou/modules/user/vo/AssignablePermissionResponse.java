package org.liangyou.modules.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "角色可分配权限树节点")
public class AssignablePermissionResponse {
    private Long id;
    private String permissionCode;
    private String permissionName;
    private Integer permissionType;
    private Long parentId;
    private Integer status;
    private List<AssignablePermissionResponse> children = new ArrayList<>();
}
