package org.liangyou.modules.auth.controller;

import java.util.Map;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.liangyou.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        return ApiResponse.success(Map.of("username", "mock-user"));
    }
}
