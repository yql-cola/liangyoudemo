package org.liangyou.common.web;

import org.junit.jupiter.api.Test;
import org.liangyou.common.exception.BusinessException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    @Test
    void businessExceptionReturnsStandardErrorBody() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DemoController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/demo").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("demo-error"));
    }

    @Test
    void businessExceptionWith401ReturnsUnauthorizedStatus() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UnauthorizedDemoController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/unauthorized").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("unauthorized-demo"));
    }

    @RestController
    static class DemoController {

        @GetMapping("/demo")
        public void demo() {
            throw new BusinessException(400, "demo-error");
        }
    }

    @RestController
    static class UnauthorizedDemoController {

        @GetMapping("/unauthorized")
        public void demo() {
            throw new BusinessException(401, "unauthorized-demo");
        }
    }
}
