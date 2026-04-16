package org.liangyou.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "新增用户请求")
public class UserCreateRequest {

    @NotBlank(message = "username-required")
    @Schema(description = "登录用户名", example = "warehouse01")
    private String username;
    @NotBlank(message = "password-required")
    @Schema(description = "登录密码", example = "123456")
    private String password;
    @NotBlank(message = "real-name-required")
    @Schema(description = "真实姓名", example = "仓库员工A")
    private String realName;
    @Schema(description = "手机号", example = "13800000000")
    private String phone;
    @Schema(description = "邮箱", example = "warehouse01@test.com")
    private String email;
    @NotNull(message = "warehouse-id-required")
    @Schema(description = "所属仓库 ID", example = "1")
    private Long warehouseId;
    @NotNull(message = "status-required")
    @Schema(description = "状态，1 启用 0 停用", example = "1")
    private Integer status;
    @Schema(description = "是否超级管理员", example = "0")
    private Integer isSuperAdmin = 0;
}
