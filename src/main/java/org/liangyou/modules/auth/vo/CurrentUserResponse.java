package org.liangyou.modules.auth.vo;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "当前登录用户信息")
public class CurrentUserResponse {

    @Schema(description = "用户 ID", example = "100")
    private Long id;

    @Schema(description = "用户名", example = "warehouse01")
    private String username;

    @Schema(description = "真实姓名", example = "仓库员工A")
    private String realName;

    @ArraySchema(schema = @Schema(description = "角色编码", example = "WAREHOUSE"))
    private List<String> roles;

    @ArraySchema(schema = @Schema(description = "权限编码", example = "system:user:view"))
    private List<String> permissions;
}
