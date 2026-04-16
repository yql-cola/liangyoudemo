package org.liangyou.modules.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "菜单树响应")
public class MenuTreeResponse {

    @Schema(description = "菜单 ID", example = "1")
    private Long id;

    @Schema(description = "菜单名称", example = "系统管理")
    private String name;

    @Schema(description = "前端路由", example = "/system")
    private String path;

    @Schema(description = "子菜单列表")
    private List<MenuTreeResponse> children = new ArrayList<>();
}
