package org.liangyou.modules.basic.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.liangyou.common.api.PageResponse;
import org.liangyou.common.web.GlobalExceptionHandler;
import org.liangyou.modules.basic.dto.ProductSaveRequest;
import org.liangyou.modules.basic.dto.StatusUpdateRequest;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.basic.service.ExcelExportService;
import org.liangyou.modules.basic.vo.BasicOptionResponse;
import org.liangyou.modules.basic.vo.ProductResponse;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void productCrudEndpointsReturnUnifiedResponses() throws Exception {
        BasicDataService service = mock(BasicDataService.class);
        ExcelExportService excelExportService = mock(ExcelExportService.class);
        ProductResponse product = new ProductResponse();
        product.setId(1L);
        product.setProductCode("P001");
        product.setProductName("东北大米");
        when(service.queryProducts(any())).thenReturn(new PageResponse<>(List.of(product), 1));
        when(service.productDetail(1L)).thenReturn(product);
        when(service.createProduct(any(ProductSaveRequest.class))).thenReturn(product);
        when(service.updateProduct(eq(1L), any(ProductSaveRequest.class))).thenReturn(product);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(service, excelExportService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        ProductSaveRequest request = new ProductSaveRequest();
        request.setProductCode("P001");
        request.setProductName("东北大米");
        request.setCategoryId(1L);
        request.setBaseUnitId(1L);

        mockMvc.perform(get("/api/v1/basic/products").param("pageNum", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].productCode").value("P001"));
        mockMvc.perform(get("/api/v1/basic/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productName").value("东北大米"));
        mockMvc.perform(post("/api/v1/basic/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productCode").value("P001"));
        mockMvc.perform(put("/api/v1/basic/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productCode").value("P001"));
        mockMvc.perform(delete("/api/v1/basic/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void productStatusOptionsAndExportEndpointsWork() throws Exception {
        BasicDataService service = mock(BasicDataService.class);
        ExcelExportService excelExportService = mock(ExcelExportService.class);
        BasicOptionResponse option = new BasicOptionResponse();
        option.setId(1L);
        option.setCode("P001");
        option.setName("东北大米");
        option.setStatus(1);
        when(service.productOptions("米")).thenReturn(List.of(option));
        when(excelExportService.exportProducts(any())).thenReturn("xlsx".getBytes());

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(service, excelExportService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(0);

        mockMvc.perform(patch("/api/v1/basic/products/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/v1/basic/products/options").param("keyword", "米"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("东北大米"));
        mockMvc.perform(get("/api/v1/basic/products/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"products.xlsx\""));
    }
}
