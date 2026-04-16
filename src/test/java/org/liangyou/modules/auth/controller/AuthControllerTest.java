package org.liangyou.modules.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.liangyou.common.web.GlobalExceptionHandler;
import org.liangyou.config.SecurityConfig;
import org.liangyou.modules.auth.dto.LoginRequest;
import org.liangyou.modules.auth.service.AuthService;
import org.liangyou.modules.user.entity.SysUser;
import org.liangyou.modules.user.service.UserService;
import org.liangyou.security.JwtAuthenticationFilter;
import org.liangyou.security.JwtTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, AuthService.class, JwtTokenService.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private UserService userService;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    @Test
    void loginReturnsJwtAndUserInfo() throws Exception {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("warehouse01");
        user.setPassword("123456");
        user.setRealName("仓库员工A");
        user.setStatus(1);
        user.setIsSuperAdmin(0);

        when(userService.findByUsername("warehouse01")).thenReturn(user);
        when(userService.loadLoginAuthorities(1L)).thenReturn(new UserService.LoginAuthorities(
                List.of("WAREHOUSE"),
                List.of("system:user:view", "system:user:list")));

        LoginRequest request = new LoginRequest();
        request.setUsername("warehouse01");
        request.setPassword("123456");

        String token = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(7200))
                .andExpect(jsonPath("$.data.userInfo.username").value("warehouse01"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(token);
        String jwt = root.path("data").path("token").asText();
        String payload = new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), StandardCharsets.UTF_8);
        JsonNode claims = objectMapper.readTree(payload);
        org.junit.jupiter.api.Assertions.assertEquals("warehouse01", claims.path("sub").asText());
        org.junit.jupiter.api.Assertions.assertTrue(claims.path("roles").toString().contains("WAREHOUSE"));
    }

    @Test
    void meReturnsCurrentUserForValidToken() throws Exception {
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);

        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("warehouse01");
        user.setRealName("仓库员工A");
        when(userService.loadLoginAuthorities(1L)).thenReturn(new UserService.LoginAuthorities(
                List.of("WAREHOUSE"),
                List.of("system:user:view")));

        String token = issueTokenForTest();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("warehouse01"));
    }

    @Test
    void logoutRevokesBearerToken() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);

        String token = issueTokenForTest();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        var keyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        var valueCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        var durationCaptor = org.mockito.ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), durationCaptor.capture());
        assertTrue(keyCaptor.getValue().startsWith("auth:jwt:revoked:"));
        assertFalse(keyCaptor.getValue().contains(token));
        assertEquals("revoked", valueCaptor.getValue());
        assertTrue(durationCaptor.getValue().getSeconds() > 0);
        assertTrue(durationCaptor.getValue().getSeconds() <= 7200);
    }

    @Test
    void badPasswordReturnsValidationStyleError() throws Exception {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("warehouse01");
        user.setPassword("123456");
        user.setRealName("仓库员工A");
        user.setStatus(1);
        when(userService.findByUsername("warehouse01")).thenReturn(user);

        LoginRequest request = new LoginRequest();
        request.setUsername("warehouse01");
        request.setPassword("wrong");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    private String issueTokenForTest() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("warehouse01");
        request.setPassword("123456");
        when(userService.findByUsername("warehouse01")).thenReturn(createUser());
        when(userService.loadLoginAuthorities(1L)).thenReturn(new UserService.LoginAuthorities(
                List.of("WAREHOUSE"),
                List.of("system:user:view")));

        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).path("data").path("token").asText();
    }

    @Test
    void revokedTokenIsRejectedOnMe() throws Exception {
        String token = issueTokenForTest();
        when(stringRedisTemplate.hasKey(jwtTokenService.buildRevokedTokenKey(token))).thenReturn(true);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录或 Token 无效"));
    }

    private SysUser createUser() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("warehouse01");
        user.setPassword("123456");
        user.setRealName("仓库员工A");
        user.setStatus(1);
        user.setIsSuperAdmin(0);
        return user;
    }
}
