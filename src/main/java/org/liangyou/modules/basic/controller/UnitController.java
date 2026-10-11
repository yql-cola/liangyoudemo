package org.liangyou.modules.basic.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.ApiResponse;
import org.liangyou.modules.basic.dto.BasicQueryRequest;
import org.liangyou.modules.basic.dto.StatusUpdateRequest;
import org.liangyou.modules.basic.dto.UnitSaveRequest;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.basic.service.ExcelExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/basic/units")
@RequiredArgsConstructor
public class UnitController {

    private final BasicDataService service;
    private final ExcelExportService excelExportService;

    @GetMapping public ApiResponse<?> query(BasicQueryRequest request) { return ApiResponse.success(service.queryUnits(request)); }
    @GetMapping("/{id}") public ApiResponse<?> detail(@PathVariable Long id) { return ApiResponse.success(service.unitDetail(id)); }
    @PostMapping public ApiResponse<?> create(@Valid @RequestBody UnitSaveRequest request) { return ApiResponse.success(service.createUnit(request)); }
    @PutMapping("/{id}") public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody UnitSaveRequest request) { return ApiResponse.success(service.updateUnit(id, request)); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable Long id) { service.deleteUnit(id); return ApiResponse.success(null); }
    @PatchMapping("/{id}/status") public ApiResponse<Void> status(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) { service.updateUnitStatus(id, request); return ApiResponse.success(null); }
    @GetMapping("/options") public ApiResponse<?> options(String keyword) { return ApiResponse.success(service.unitOptions(keyword)); }
    @GetMapping("/export") public ResponseEntity<byte[]> export() { return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"units.xlsx\"").body(excelExportService.exportEmptySheet("units")); }
}
