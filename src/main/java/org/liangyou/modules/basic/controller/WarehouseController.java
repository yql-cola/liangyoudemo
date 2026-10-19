package org.liangyou.modules.basic.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.ApiResponse;
import org.liangyou.common.api.PageResponse;
import org.liangyou.modules.basic.dto.BasicQueryRequest;
import org.liangyou.modules.basic.dto.WarehouseSaveRequest;
import org.liangyou.modules.basic.dto.StatusUpdateRequest;
import org.liangyou.modules.basic.entity.BaseWarehouse;
import org.liangyou.modules.basic.vo.BasicOptionResponse;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.basic.service.ExcelExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Basic Warehouse")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/basic/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final BasicDataService service;
    private final ExcelExportService excelExportService;

    @GetMapping
    @Operation(summary = "分页查询仓库")
    public ApiResponse<PageResponse<BaseWarehouse>> query(@Valid BasicQueryRequest request) { return ApiResponse.success(service.queryWarehouses(request)); }

    @GetMapping("/{id}")
    @Operation(summary = "查询仓库详情")
    public ApiResponse<BaseWarehouse> detail(@PathVariable("id") Long id) { return ApiResponse.success(service.warehouseDetail(id)); }

    @PostMapping
    @Operation(summary = "新增仓库")
    public ApiResponse<BaseWarehouse> create(@Valid @RequestBody WarehouseSaveRequest request) { return ApiResponse.success(service.createWarehouse(request)); }

    @PutMapping("/{id}")
    @Operation(summary = "修改仓库")
    public ApiResponse<BaseWarehouse> update(@PathVariable("id") Long id, @Valid @RequestBody WarehouseSaveRequest request) { return ApiResponse.success(service.updateWarehouse(id, request)); }

    @DeleteMapping("/{id}")
    @Operation(summary = "逻辑删除仓库（已引用数据不可删除）")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) { service.deleteWarehouse(id); return ApiResponse.success(null); }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启停用仓库")
    public ApiResponse<Void> status(@PathVariable("id") Long id, @Valid @RequestBody StatusUpdateRequest request) { service.updateWarehouseStatus(id, request); return ApiResponse.success(null); }

    @GetMapping("/options")
    @Operation(summary = "查询启用的仓库选项")
    public ApiResponse<List<BasicOptionResponse>> options(String keyword) { return ApiResponse.success(service.warehouseOptions(keyword)); }

    @GetMapping("/export")
    @Operation(summary = "导出全部符合筛选条件的仓库")
    public ResponseEntity<byte[]> export(@Valid BasicQueryRequest request) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"warehouses.xlsx\"")
                .body(excelExportService.exportBasic("warehouses", request));
    }
}
