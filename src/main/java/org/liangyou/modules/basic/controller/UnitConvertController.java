package org.liangyou.modules.basic.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.ApiResponse;
import org.liangyou.modules.basic.dto.BasicQueryRequest;
import org.liangyou.modules.basic.dto.StatusUpdateRequest;
import org.liangyou.modules.basic.dto.UnitConvertSaveRequest;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.basic.service.ExcelExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/basic/unit-converts")
@RequiredArgsConstructor
public class UnitConvertController {

    private final BasicDataService service;
    private final ExcelExportService excelExportService;

    @GetMapping public ApiResponse<?> query(BasicQueryRequest request) { return ApiResponse.success(service.queryUnitConverts(request)); }
    @GetMapping("/{id}") public ApiResponse<?> detail(@PathVariable Long id) { return ApiResponse.success(service.unitConvertDetail(id)); }
    @PostMapping public ApiResponse<?> create(@Valid @RequestBody UnitConvertSaveRequest request) { return ApiResponse.success(service.createUnitConvert(request)); }
    @PutMapping("/{id}") public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody UnitConvertSaveRequest request) { return ApiResponse.success(service.updateUnitConvert(id, request)); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable Long id) { service.deleteUnitConvert(id); return ApiResponse.success(null); }
    @PatchMapping("/{id}/status") public ApiResponse<Void> status(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) { service.updateUnitConvertStatus(id, request); return ApiResponse.success(null); }
    @GetMapping("/options") public ApiResponse<?> options(String keyword) { return ApiResponse.success(service.unitConvertOptions(keyword)); }
    @GetMapping("/export") public ResponseEntity<byte[]> export() { return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"unit-converts.xlsx\"").body(excelExportService.exportEmptySheet("unit-converts")); }
}
