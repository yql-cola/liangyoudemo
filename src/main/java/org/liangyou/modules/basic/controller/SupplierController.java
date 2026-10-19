package org.liangyou.modules.basic.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.ApiResponse;
import org.liangyou.common.api.PageResponse;
import org.liangyou.modules.basic.dto.BasicQueryRequest;
import org.liangyou.modules.basic.dto.SupplierSaveRequest;
import org.liangyou.modules.basic.dto.StatusUpdateRequest;
import org.liangyou.modules.basic.entity.BaseSupplier;
import org.liangyou.modules.basic.vo.BasicOptionResponse;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.basic.service.ExcelExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Basic Supplier")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/basic/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final BasicDataService service;
    private final ExcelExportService excelExportService;

    @GetMapping
    @Operation(summary = "分页查询供应商")
    public ApiResponse<PageResponse<BaseSupplier>> query(@Valid BasicQueryRequest request) { return ApiResponse.success(service.querySuppliers(request)); }

    @GetMapping("/{id}")
    @Operation(summary = "查询供应商详情")
    public ApiResponse<BaseSupplier> detail(@PathVariable("id") Long id) { return ApiResponse.success(service.supplierDetail(id)); }

    @PostMapping
    @Operation(summary = "新增供应商")
    public ApiResponse<BaseSupplier> create(@Valid @RequestBody SupplierSaveRequest request) { return ApiResponse.success(service.createSupplier(request)); }

    @PutMapping("/{id}")
    @Operation(summary = "修改供应商")
    public ApiResponse<BaseSupplier> update(@PathVariable("id") Long id, @Valid @RequestBody SupplierSaveRequest request) { return ApiResponse.success(service.updateSupplier(id, request)); }

    @DeleteMapping("/{id}")
    @Operation(summary = "逻辑删除供应商（已引用数据不可删除）")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) { service.deleteSupplier(id); return ApiResponse.success(null); }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启停用供应商")
    public ApiResponse<Void> status(@PathVariable("id") Long id, @Valid @RequestBody StatusUpdateRequest request) { service.updateSupplierStatus(id, request); return ApiResponse.success(null); }

    @GetMapping("/options")
    @Operation(summary = "查询启用的供应商选项")
    public ApiResponse<List<BasicOptionResponse>> options(String keyword) { return ApiResponse.success(service.supplierOptions(keyword)); }

    @GetMapping("/export")
    @Operation(summary = "导出全部符合筛选条件的供应商")
    public ResponseEntity<byte[]> export(@Valid BasicQueryRequest request) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"suppliers.xlsx\"")
                .body(excelExportService.exportBasic("suppliers", request));
    }
}
