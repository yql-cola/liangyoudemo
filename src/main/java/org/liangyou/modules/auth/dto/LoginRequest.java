package org.liangyou.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "登录请求")
public class LoginRequest {

    @NotBlank(message = "username-required")
    @Schema(description = "用户名", example = "warehouse01")
    private String username;

    @NotBlank(message = "password-required")
    @Schema(description = "密码", example = "123456")
    private String password;
}
