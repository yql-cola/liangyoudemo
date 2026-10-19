package org.liangyou.modules.basic.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.ApiResponse;
import org.liangyou.common.api.PageResponse;
import org.liangyou.modules.basic.dto.BasicQueryRequest;
import org.liangyou.modules.basic.dto.CustomerSaveRequest;
import org.liangyou.modules.basic.dto.StatusUpdateRequest;
import org.liangyou.modules.basic.entity.BaseCustomer;
import org.liangyou.modules.basic.vo.BasicOptionResponse;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.basic.service.ExcelExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Basic Customer")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/basic/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final BasicDataService service;
    private final ExcelExportService excelExportService;

    @GetMapping
    @Operation(summary = "分页查询客户")
    public ApiResponse<PageResponse<BaseCustomer>> query(@Valid BasicQueryRequest request) { return ApiResponse.success(service.queryCustomers(request)); }

    @GetMapping("/{id}")
    @Operation(summary = "查询客户详情")
    public ApiResponse<BaseCustomer> detail(@PathVariable("id") Long id) { return ApiResponse.success(service.customerDetail(id)); }

    @PostMapping
    @Operation(summary = "新增客户")
    public ApiResponse<BaseCustomer> create(@Valid @RequestBody CustomerSaveRequest request) { return ApiResponse.success(service.createCustomer(request)); }

    @PutMapping("/{id}")
    @Operation(summary = "修改客户")
    public ApiResponse<BaseCustomer> update(@PathVariable("id") Long id, @Valid @RequestBody CustomerSaveRequest request) { return ApiResponse.success(service.updateCustomer(id, request)); }

    @DeleteMapping("/{id}")
    @Operation(summary = "逻辑删除客户（已引用数据不可删除）")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) { service.deleteCustomer(id); return ApiResponse.success(null); }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启停用客户")
    public ApiResponse<Void> status(@PathVariable("id") Long id, @Valid @RequestBody StatusUpdateRequest request) { service.updateCustomerStatus(id, request); return ApiResponse.success(null); }

    @GetMapping("/options")
    @Operation(summary = "查询启用的客户选项")
    public ApiResponse<List<BasicOptionResponse>> options(String keyword) { return ApiResponse.success(service.customerOptions(keyword)); }

    @GetMapping("/export")
    @Operation(summary = "导出全部符合筛选条件的客户")
    public ResponseEntity<byte[]> export(@Valid BasicQueryRequest request) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"customers.xlsx\"")
                .body(excelExportService.exportBasic("customers", request));
    }
}
