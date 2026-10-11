package org.liangyou.modules.basic.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.ApiResponse;
import org.liangyou.modules.basic.dto.BasicQueryRequest;
import org.liangyou.modules.basic.dto.StatusUpdateRequest;
import org.liangyou.modules.basic.dto.SupplierSaveRequest;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.basic.service.ExcelExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/basic/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final BasicDataService service;
    private final ExcelExportService excelExportService;

    @GetMapping public ApiResponse<?> query(BasicQueryRequest request) { return ApiResponse.success(service.querySuppliers(request)); }
    @GetMapping("/{id}") public ApiResponse<?> detail(@PathVariable Long id) { return ApiResponse.success(service.supplierDetail(id)); }
    @PostMapping public ApiResponse<?> create(@Valid @RequestBody SupplierSaveRequest request) { return ApiResponse.success(service.createSupplier(request)); }
    @PutMapping("/{id}") public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody SupplierSaveRequest request) { return ApiResponse.success(service.updateSupplier(id, request)); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable Long id) { service.deleteSupplier(id); return ApiResponse.success(null); }
    @PatchMapping("/{id}/status") public ApiResponse<Void> status(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) { service.updateSupplierStatus(id, request); return ApiResponse.success(null); }
    @GetMapping("/options") public ApiResponse<?> options(String keyword) { return ApiResponse.success(service.supplierOptions(keyword)); }
    @GetMapping("/export") public ResponseEntity<byte[]> export() { return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"suppliers.xlsx\"").body(excelExportService.exportEmptySheet("suppliers")); }
}
