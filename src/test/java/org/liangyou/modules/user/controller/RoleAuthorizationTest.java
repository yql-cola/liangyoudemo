package org.liangyou.modules.user.controller;

import org.junit.jupiter.api.Test;
import org.liangyou.common.web.GlobalExceptionHandler;
import org.liangyou.config.SecurityConfig;
import org.liangyou.modules.user.dto.RoleCreateRequest;
import org.liangyou.modules.user.service.UserService;
import org.liangyou.security.AuthenticatedUserPrincipal;
import org.liangyou.security.JwtAuthenticationFilter;
import org.liangyou.security.JwtTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoleController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtTokenService.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class RoleAuthorizationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenService jwtTokenService;
    @MockBean private UserService userService;
    @MockBean private StringRedisTemplate stringRedisTemplate;

    @Test
    void createWithGrantFieldReturnsForbiddenWhenServiceRejectsExtraPrivilege() throws Exception {
        when(stringRedisTemplate.hasKey(any())).thenReturn(false);
        when(userService.createRole(any(RoleCreateRequest.class)))
                .thenThrow(new AccessDeniedException("无权限访问"));
        String token = jwtTokenService.issueToken(new AuthenticatedUserPrincipal(
                1L, "creator", "创建人", List.of("STAFF"), List.of("system:role:create")));

        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"roleCode\":\"STAFF\",\"roleName\":\"员工\",\"permissionIds\":[]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
