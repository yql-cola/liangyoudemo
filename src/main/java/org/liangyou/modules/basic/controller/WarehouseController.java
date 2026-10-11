package org.liangyou.modules.basic.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.ApiResponse;
import org.liangyou.modules.basic.dto.BasicQueryRequest;
import org.liangyou.modules.basic.dto.StatusUpdateRequest;
import org.liangyou.modules.basic.dto.WarehouseSaveRequest;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.basic.service.ExcelExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/basic/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final BasicDataService service;
    private final ExcelExportService excelExportService;

    @GetMapping public ApiResponse<?> query(BasicQueryRequest request) { return ApiResponse.success(service.queryWarehouses(request)); }
    @GetMapping("/{id}") public ApiResponse<?> detail(@PathVariable Long id) { return ApiResponse.success(service.warehouseDetail(id)); }
    @PostMapping public ApiResponse<?> create(@Valid @RequestBody WarehouseSaveRequest request) { return ApiResponse.success(service.createWarehouse(request)); }
    @PutMapping("/{id}") public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody WarehouseSaveRequest request) { return ApiResponse.success(service.updateWarehouse(id, request)); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable Long id) { service.deleteWarehouse(id); return ApiResponse.success(null); }
    @PatchMapping("/{id}/status") public ApiResponse<Void> status(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) { service.updateWarehouseStatus(id, request); return ApiResponse.success(null); }
    @GetMapping("/options") public ApiResponse<?> options(String keyword) { return ApiResponse.success(service.warehouseOptions(keyword)); }
    @GetMapping("/export") public ResponseEntity<byte[]> export() { return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"warehouses.xlsx\"").body(excelExportService.exportEmptySheet("warehouses")); }
}
