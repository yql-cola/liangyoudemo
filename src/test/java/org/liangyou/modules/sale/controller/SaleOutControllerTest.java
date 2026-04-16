package org.liangyou.modules.sale.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.liangyou.common.web.GlobalExceptionHandler;
import org.liangyou.modules.sale.dto.SaleOutCreateRequest;
import org.liangyou.modules.sale.dto.SaleOutItemRequest;
import org.liangyou.modules.sale.service.SaleOutService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SaleOutControllerTest {

    @Test
    void createSaleOutReturnsDocumentId() throws Exception {
        SaleOutService saleOutService = new SaleOutService(null, null, null, null, null, null) {
            @Override
            public Long create(SaleOutCreateRequest request) {
                return 300L;
            }
        };
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SaleOutController(saleOutService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        SaleOutItemRequest item = new SaleOutItemRequest();
        item.setProductId(1L);
        item.setUnitId(1L);
        item.setQty(new BigDecimal("2"));
        item.setUnitPrice(new BigDecimal("150"));
        item.setAmount(new BigDecimal("300"));

        SaleOutCreateRequest request = new SaleOutCreateRequest();
        request.setWarehouseId(1L);
        request.setCustomerId(1L);
        request.setBizDate(LocalDate.now());
        request.setItems(List.of(item));

        mockMvc.perform(post("/api/v1/sale-outs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(300));
    }
}
