package org.liangyou.modules.inventory.controller;

import org.junit.jupiter.api.Test;
import org.liangyou.common.web.GlobalExceptionHandler;
import org.liangyou.common.api.PageResponse;
import org.liangyou.modules.inventory.dto.InventoryQueryRequest;
import org.liangyou.modules.inventory.service.InventoryService;
import org.liangyou.modules.inventory.vo.InventoryBatchResponse;
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

    @Test
    void batchesEndpointForwardsWarehouseAndProduct() throws Exception {
        long[] capturedWarehouseId = new long[1];
        long[] capturedProductId = new long[1];
        InventoryService inventoryService = new InventoryService(null, null, null) {
            @Override
            public List<InventoryBatchResponse> batches(Long warehouseId, Long productId) {
                capturedWarehouseId[0] = warehouseId == null ? -1 : warehouseId;
                capturedProductId[0] = productId == null ? -1 : productId;
                return List.of();
            }
        };
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new InventoryController(inventoryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/inventories/42/batches").param("warehouseId", "7"))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(7L, capturedWarehouseId[0]);
        org.junit.jupiter.api.Assertions.assertEquals(42L, capturedProductId[0]);
    }

    @Test
    void oldBatchesRouteDoesNotInvokeService() throws Exception {
        boolean[] called = new boolean[1];
        InventoryService inventoryService = new InventoryService(null, null, null) {
            @Override
            public List<InventoryBatchResponse> batches(Long warehouseId, Long productId) {
                called[0] = true;
                return List.of();
            }
        };
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new InventoryController(inventoryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/inventories/batches").param("warehouseId", "7").param("productId", "42"))
                .andExpect(status().is4xxClientError());
        org.junit.jupiter.api.Assertions.assertFalse(called[0]);
    }
}
