package org.liangyou.modules.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "JWT token")
    private String token;

    @Schema(description = "token 类型", example = "Bearer")
    private String tokenType;

    @Schema(description = "token 有效期秒数", example = "7200")
    private Long expiresIn;

    @Schema(description = "用户信息")
    private CurrentUserResponse userInfo;
}
