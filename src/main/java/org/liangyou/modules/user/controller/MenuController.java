package org.liangyou.modules.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.ApiResponse;
import org.liangyou.modules.user.service.UserService;
import org.liangyou.modules.user.vo.MenuTreeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Menu")
@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class MenuController {

    private final UserService userService;

    @GetMapping("/tree")
    @Operation(summary = "查询菜单树", description = "返回当前用户可见的菜单树。权限点：system:menu:view")
    public ApiResponse<List<MenuTreeResponse>> tree() {
        return ApiResponse.success(userService.getMenuTree());
    }
}
