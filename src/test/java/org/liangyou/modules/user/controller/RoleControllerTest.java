package org.liangyou.modules.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.liangyou.common.web.GlobalExceptionHandler;
import org.liangyou.modules.user.dto.RoleCreateRequest;
import org.liangyou.modules.user.dto.RolePermissionAssignRequest;
import org.liangyou.modules.user.dto.RoleQueryRequest;
import org.liangyou.modules.user.dto.RoleUpdateRequest;
import org.liangyou.modules.user.service.UserService;
import org.liangyou.modules.user.vo.RoleResponse;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoleControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createRoleReturnsRoleCode() throws Exception {
        UserService userService = mock(UserService.class);
        when(userService.createRole(any(RoleCreateRequest.class))).thenReturn(buildRole(1L, "WAREHOUSE"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RoleController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        RoleCreateRequest request = new RoleCreateRequest();
        request.setRoleCode("WAREHOUSE");
        request.setRoleName("仓库人员");
        request.setRemark("仓库角色");

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("WAREHOUSE"));
    }

    @Test
    void updateRoleReturnsUpdatedRoleCode() throws Exception {
        UserService userService = mock(UserService.class);
        when(userService.updateRole(anyLong(), any(RoleUpdateRequest.class))).thenReturn(buildRole(1L, "FINANCE"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RoleController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setRoleCode("FINANCE");
        request.setRoleName("财务人员");
        request.setStatus(1);
        request.setRemark("财务角色");

        mockMvc.perform(put("/api/v1/roles/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("FINANCE"));
    }

    @Test
    void deleteRoleReturnsSuccess() throws Exception {
        UserService userService = mock(UserService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RoleController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(delete("/api/v1/roles/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void listRolesReturnsFilteredRows() throws Exception {
        UserService userService = mock(UserService.class);
        when(userService.listRoles(any(RoleQueryRequest.class))).thenReturn(List.of(
                buildRole(1L, "WAREHOUSE"),
                buildRole(2L, "FINANCE")));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RoleController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/roles")
                        .param("roleName", "仓库")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].roleCode").value("WAREHOUSE"))
                .andExpect(jsonPath("$.data[1].roleCode").value("FINANCE"));
    }

    @Test
    void assignRolePermissionsReturnsSuccess() throws Exception {
        UserService userService = mock(UserService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RoleController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        RolePermissionAssignRequest request = new RolePermissionAssignRequest();
        request.setPermissionIds(List.of(101L, 102L));

        mockMvc.perform(post("/api/v1/roles/{id}/permissions", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private RoleResponse buildRole(Long id, String roleCode) {
        RoleResponse response = new RoleResponse();
        response.setId(id);
        response.setRoleCode(roleCode);
        response.setRoleName(roleCode + "-name");
        response.setStatus(1);
        response.setRemark(roleCode + "-remark");
        return response;
    }
}
