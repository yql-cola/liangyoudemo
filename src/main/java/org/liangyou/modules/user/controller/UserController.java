package org.liangyou.modules.user.controller;

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
public class UserController {

    private final UserService userService;

    @PostMapping
    public ApiResponse<Long> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userService.createUser(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        userService.updateUser(id, request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}")
    public ApiResponse<UserDetailResponse> getUserDetail(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserDetail(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<UserDetailResponse>> queryUsers(UserQueryRequest request) {
        return ApiResponse.success(userService.queryUsers(request));
    }

    @PostMapping("/{id}/roles")
    public ApiResponse<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody UserRoleAssignRequest request) {
        userService.assignRoles(id, request);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/permissions")
    public ApiResponse<Void> assignUserPermissions(@PathVariable Long id,
                                                   @RequestBody UserPermissionAssignRequest request) {
        userService.assignUserPermissions(id, request);
        return ApiResponse.success(null);
    }

    @PostMapping("/roles/{roleId}/permissions")
    public ApiResponse<Void> assignRolePermissions(@PathVariable Long roleId,
                                                   @Valid @RequestBody RolePermissionAssignRequest request) {
        userService.assignRolePermissions(roleId, request);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/permissions")
    public ApiResponse<UserPermissionResponse> getUserPermissions(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserPermissions(id));
    }
}
