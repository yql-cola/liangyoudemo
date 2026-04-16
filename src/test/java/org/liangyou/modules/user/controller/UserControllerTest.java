package org.liangyou.modules.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.liangyou.common.web.GlobalExceptionHandler;
import org.liangyou.modules.user.dto.UserCreateRequest;
import org.liangyou.modules.user.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    @Test
    void createUserReturnsCreatedUserId() throws Exception {
        UserService userService = new UserService(null, null, null, null, null, null) {
            @Override
            public Long createUser(UserCreateRequest request) {
                return 100L;
            }
        };
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        ObjectMapper objectMapper = new ObjectMapper();

        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("warehouse01");
        request.setPassword("123456");
        request.setRealName("仓库A");
        request.setWarehouseId(1L);
        request.setStatus(1);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(100));
    }
}
