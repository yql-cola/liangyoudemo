package org.liangyou.modules.basic.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.liangyou.common.web.GlobalExceptionHandler;
import org.liangyou.modules.basic.dto.CategorySaveRequest;
import org.liangyou.modules.basic.dto.CustomerSaveRequest;
import org.liangyou.modules.basic.dto.StatusUpdateRequest;
import org.liangyou.modules.basic.dto.SupplierSaveRequest;
import org.liangyou.modules.basic.dto.UnitConvertSaveRequest;
import org.liangyou.modules.basic.dto.UnitSaveRequest;
import org.liangyou.modules.basic.dto.WarehouseSaveRequest;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.basic.service.ExcelExportService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BasicResourceControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void categoryUnitWarehouseSupplierCustomerAndUnitConvertExposeCrudSkeletons() throws Exception {
        BasicDataService service = mock(BasicDataService.class);
        ExcelExportService excelExportService = mock(ExcelExportService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new CategoryController(service, excelExportService),
                        new UnitController(service, excelExportService),
                        new WarehouseController(service, excelExportService),
                        new SupplierController(service, excelExportService),
                        new CustomerController(service, excelExportService),
                        new UnitConvertController(service, excelExportService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        assertResourceEndpoints(mockMvc, "/api/v1/basic/categories", categoryRequest());
        assertResourceEndpoints(mockMvc, "/api/v1/basic/units", unitRequest());
        assertResourceEndpoints(mockMvc, "/api/v1/basic/warehouses", warehouseRequest());
        assertResourceEndpoints(mockMvc, "/api/v1/basic/suppliers", supplierRequest());
        assertResourceEndpoints(mockMvc, "/api/v1/basic/customers", customerRequest());
        assertResourceEndpoints(mockMvc, "/api/v1/basic/unit-converts", unitConvertRequest());
    }

    private void assertResourceEndpoints(MockMvc mockMvc, String path, Object body) throws Exception {
        StatusUpdateRequest status = new StatusUpdateRequest();
        status.setStatus(0);

        mockMvc.perform(get(path).param("pageNum", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get(path + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(delete(path + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(patch(path + "/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(status)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get(path + "/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get(path + "/export"))
                .andExpect(status().isOk());
    }

    private CategorySaveRequest categoryRequest() {
        CategorySaveRequest request = new CategorySaveRequest();
        request.setCategoryName("大米");
        request.setParentId(0L);
        request.setSortNo(1);
        return request;
    }

    private UnitSaveRequest unitRequest() {
        UnitSaveRequest request = new UnitSaveRequest();
        request.setUnitCode("KG");
        request.setUnitName("公斤");
        return request;
    }

    private WarehouseSaveRequest warehouseRequest() {
        WarehouseSaveRequest request = new WarehouseSaveRequest();
        request.setWarehouseCode("WH001");
        request.setWarehouseName("总仓");
        return request;
    }

    private SupplierSaveRequest supplierRequest() {
        SupplierSaveRequest request = new SupplierSaveRequest();
        request.setSupplierCode("S001");
        request.setSupplierName("粮油供应商");
        return request;
    }

    private CustomerSaveRequest customerRequest() {
        CustomerSaveRequest request = new CustomerSaveRequest();
        request.setCustomerCode("C001");
        request.setCustomerName("门店客户");
        return request;
    }

    private UnitConvertSaveRequest unitConvertRequest() {
        UnitConvertSaveRequest request = new UnitConvertSaveRequest();
        request.setProductId(1L);
        request.setFromUnitId(2L);
        request.setToUnitId(1L);
        request.setConvertRate(new BigDecimal("10"));
        return request;
    }
}
