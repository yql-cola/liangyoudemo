package org.liangyou.modules.user.controller;

import org.junit.jupiter.api.Test;
import org.liangyou.common.web.GlobalExceptionHandler;
import org.liangyou.modules.user.service.UserService;
import org.liangyou.modules.user.vo.MenuTreeResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MenuControllerTest {

    @Test
    void menuTreeReturnsNestedChildren() throws Exception {
        UserService userService = mock(UserService.class);
        when(userService.getMenuTree()).thenReturn(List.of(buildMenuTree()));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MenuController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/menus/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("系统管理"))
                .andExpect(jsonPath("$.data[0].children[0].name").value("用户管理"));
    }

    private MenuTreeResponse buildMenuTree() {
        MenuTreeResponse child = new MenuTreeResponse();
        child.setId(2L);
        child.setName("用户管理");
        child.setPath("/users");

        MenuTreeResponse parent = new MenuTreeResponse();
        parent.setId(1L);
        parent.setName("系统管理");
        parent.setPath("/system");
        parent.setChildren(List.of(child));
        return parent;
    }
}
