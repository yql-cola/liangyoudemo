package org.liangyou.modules.inventory.controller;

import org.junit.jupiter.api.Test;
import org.liangyou.common.api.PageResponse;
import org.liangyou.common.web.GlobalExceptionHandler;
import org.liangyou.modules.inventory.dto.InventoryQueryRequest;
import org.liangyou.modules.inventory.service.InventoryService;
import org.liangyou.modules.inventory.vo.InventoryResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InventoryControllerTest {

    @Test
    void listInventoryEndpointExists() throws Exception {
        InventoryService inventoryService = new InventoryService(null, null, null) {
            @Override
            public PageResponse<InventoryResponse> query(InventoryQueryRequest request) {
                return new PageResponse<>(List.of(), 0);
            }
        };
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new InventoryController(inventoryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/inventories"))
                .andExpect(status().isOk());
    }
}
