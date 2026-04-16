package org.liangyou.modules.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.ApiResponse;
import org.liangyou.common.api.PageResponse;
import org.liangyou.modules.user.dto.RolePermissionAssignRequest;
import org.liangyou.modules.user.dto.UserCreateRequest;
import org.liangyou.modules.user.dto.UserPermissionAssignRequest;
import org.liangyou.modules.user.dto.UserQueryRequest;
import org.liangyou.modules.user.dto.UserRoleAssignRequest;
import org.liangyou.modules.user.dto.UserUpdateRequest;
import org.liangyou.modules.user.service.UserService;
import org.liangyou.modules.user.vo.UserDetailResponse;
import org.liangyou.modules.user.vo.UserPermissionResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User & Permission")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "新增用户", description = "创建仓库、财务或超级管理员用户。权限点：system:user:create")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功", content = @Content(
            examples = @ExampleObject(value = "{\"code\":0,\"message\":\"success\",\"data\":100}")
    ))
    public ApiResponse<Long> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userService.createUser(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改用户", description = "更新用户基础信息。权限点：system:user:update")
    public ApiResponse<Void> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        userService.updateUser(id, request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "逻辑删除用户。权限点：system:user:delete")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询用户详情", description = "查询单个用户的基础信息和角色绑定。权限点：system:user:view")
    public ApiResponse<UserDetailResponse> getUserDetail(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserDetail(id));
    }

    @GetMapping
    @Operation(summary = "分页查询用户", description = "按用户名、姓名、仓库和状态分页筛选。权限点：system:user:list")
    public ApiResponse<PageResponse<UserDetailResponse>> queryUsers(UserQueryRequest request) {
        return ApiResponse.success(userService.queryUsers(request));
    }

    @PostMapping("/{id}/roles")
    @Operation(summary = "给用户分配角色", description = "覆盖式更新用户角色绑定。权限点：system:user:grant-role")
    public ApiResponse<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody UserRoleAssignRequest request) {
        userService.assignRoles(id, request);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/permissions")
    @Operation(summary = "给用户单独授权", description = "支持对用户做额外授权或回收权限。权限点：system:user:grant-permission")
    public ApiResponse<Void> assignUserPermissions(@PathVariable Long id,
                                                   @RequestBody UserPermissionAssignRequest request) {
        userService.assignUserPermissions(id, request);
        return ApiResponse.success(null);
    }

    @PostMapping("/roles/{roleId}/permissions")
    @Operation(summary = "给角色分配权限", description = "设置角色拥有的权限集合。权限点：system:role:grant")
    public ApiResponse<Void> assignRolePermissions(@PathVariable Long roleId,
                                                   @Valid @RequestBody RolePermissionAssignRequest request) {
        userService.assignRolePermissions(roleId, request);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/permissions")
    @Operation(summary = "查询用户最终权限", description = "返回角色权限和用户直接授权合并后的最终权限集合。权限点：system:user:view-permission")
    public ApiResponse<UserPermissionResponse> getUserPermissions(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserPermissions(id));
    }
}
