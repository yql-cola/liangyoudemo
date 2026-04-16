package org.liangyou.modules.auth.controller;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @GetMapping("/me")
    @Operation(
            summary = "获取当前登录用户信息",
            description = "用于验证认证链路和当前登录上下文。当前项目尚未接入真实登录态时返回 mock 数据。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(
                            examples = @ExampleObject(value = """
                                    {"code":0,"message":"success","data":{"username":"mock-user"}}
                                    """))),
                    @ApiResponse(responseCode = "401", description = "未登录或 Token 无效")
            }
    )
    public org.liangyou.common.api.ApiResponse<Map<String, Object>> me() {
        return org.liangyou.common.api.ApiResponse.success(Map.of("username", "mock-user"));
    }
}
