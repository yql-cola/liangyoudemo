package org.liangyou.modules.purchase.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.liangyou.common.web.GlobalExceptionHandler;
import org.liangyou.modules.purchase.dto.PurchaseInCreateRequest;
import org.liangyou.modules.purchase.dto.PurchaseInItemRequest;
import org.liangyou.modules.purchase.service.PurchaseInService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PurchaseInControllerTest {

    @Test
    void createPurchaseInReturnsDocumentId() throws Exception {
        PurchaseInService purchaseInService = new PurchaseInService(null, null, null, null, null) {
            @Override
            public Long create(PurchaseInCreateRequest request) {
                return 200L;
            }
        };
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new PurchaseInController(purchaseInService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        PurchaseInItemRequest item = new PurchaseInItemRequest();
        item.setProductId(1L);
        item.setUnitId(1L);
        item.setQty(new BigDecimal("10"));
        item.setUnitPrice(new BigDecimal("100"));
        item.setAmount(new BigDecimal("1000"));
        item.setBatchNo("B001");
        item.setProductionDate(LocalDate.now());
        item.setExpiryDate(LocalDate.now().plusMonths(6));
        item.setInboundTime(LocalDateTime.now());

        PurchaseInCreateRequest request = new PurchaseInCreateRequest();
        request.setWarehouseId(1L);
        request.setSupplierId(1L);
        request.setBizDate(LocalDate.now());
        request.setItems(List.of(item));

        mockMvc.perform(post("/api/v1/purchase-ins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(200));
    }
}
