package org.liangyou.security;

import org.junit.jupiter.api.Test;
import org.liangyou.common.api.ApiResponse;
import org.liangyou.config.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResourcePermissionSecurityTest.ProbeController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ResourcePermissionSecurityTest.ProbeController.class})
class ResourcePermissionSecurityTest {
    @Autowired private MockMvc mvc;
    @MockBean private JwtTokenService tokenService;
    @MockBean private StringRedisTemplate redis;

    @Test void authenticatedInventoryReaderCannotExportOrAdjust() throws Exception {
        mvc.perform(get("/api/v1/inventories").with(user("reader").authorities(new SimpleGrantedAuthority("inventory:stock:list"))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/inventories/export").with(user("reader").authorities(new SimpleGrantedAuthority("inventory:stock:list"))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
        mvc.perform(put("/api/v1/inventories/1").with(user("reader").authorities(new SimpleGrantedAuthority("inventory:stock:list"))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
    }

    @Test void businessAndSystemEndpointsUseResourceAuthorization() throws Exception {
        for (String path : new String[]{"/api/v1/purchase-ins", "/api/v1/sale-outs", "/api/v1/users", "/api/v1/roles/1/permissions"}) {
            mvc.perform(post(path).with(user("reader")))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
        }
        mvc.perform(get("/api/v1/menus/tree").with(user("reader")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
        mvc.perform(post("/api/v1/roles/1/permissions").with(user("admin").authorities(new SimpleGrantedAuthority("system:role:grant"))))
                .andExpect(status().isOk());
    }

    @Test void anonymousRequestsReturnJson401WhileLoginAndDocsRemainPublic() throws Exception {
        mvc.perform(get("/api/v1/inventories"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        mvc.perform(get("/api/v1/auth/login")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").with(user("reader"))).andExpect(status().isOk());
    }

    @RestController
    static class ProbeController {
        @RequestMapping({"/api/v1/inventories", "/api/v1/inventories/export", "/api/v1/inventories/1",
                "/api/v1/purchase-ins", "/api/v1/sale-outs", "/api/v1/users", "/api/v1/roles/1/permissions",
                "/api/v1/menus/tree", "/api/v1/auth/login", "/api/v1/auth/me", "/v3/api-docs"})
        ApiResponse<Void> respond() { return ApiResponse.success(null); }
    }
}
