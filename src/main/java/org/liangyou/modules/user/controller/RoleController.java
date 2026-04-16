package org.liangyou.modules.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.ApiResponse;
import org.liangyou.modules.user.dto.RoleCreateRequest;
import org.liangyou.modules.user.dto.RolePermissionAssignRequest;
import org.liangyou.modules.user.dto.RoleQueryRequest;
import org.liangyou.modules.user.dto.RoleUpdateRequest;
import org.liangyou.modules.user.service.UserService;
import org.liangyou.modules.user.vo.RoleResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Role")
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class RoleController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "新增角色", description = "创建一个默认启用的角色。权限点：system:role:create")
    public ApiResponse<RoleResponse> create(@Valid @RequestBody RoleCreateRequest request) {
        return ApiResponse.success(userService.createRole(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改角色", description = "更新角色编码、名称、状态和备注。权限点：system:role:update")
    public ApiResponse<RoleResponse> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return ApiResponse.success(userService.updateRole(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色", description = "逻辑删除角色。权限点：system:role:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.deleteRole(id);
        return ApiResponse.success(null);
    }

    @GetMapping
    @Operation(summary = "查询角色列表", description = "按名称、编码和状态过滤角色。权限点：system:role:list")
    public ApiResponse<List<RoleResponse>> list(RoleQueryRequest request) {
        return ApiResponse.success(userService.listRoles(request));
    }

    @PostMapping("/{id}/permissions")
    @Operation(summary = "给角色分配权限", description = "覆盖式更新角色权限集合。权限点：system:role:grant")
    public ApiResponse<Void> assignPermissions(@PathVariable Long id,
                                               @Valid @RequestBody RolePermissionAssignRequest request) {
        userService.assignRolePermissions(id, request);
        return ApiResponse.success(null);
    }
}
