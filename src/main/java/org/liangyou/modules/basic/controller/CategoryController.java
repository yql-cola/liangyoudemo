package org.liangyou.modules.basic.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.ApiResponse;
import org.liangyou.modules.basic.dto.BasicQueryRequest;
import org.liangyou.modules.basic.dto.CategorySaveRequest;
import org.liangyou.modules.basic.dto.StatusUpdateRequest;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.basic.service.ExcelExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/basic/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final BasicDataService service;
    private final ExcelExportService excelExportService;

    @GetMapping public ApiResponse<?> query(BasicQueryRequest request) { return ApiResponse.success(service.queryCategories(request)); }
    @GetMapping("/{id}") public ApiResponse<?> detail(@PathVariable Long id) { return ApiResponse.success(service.categoryDetail(id)); }
    @PostMapping public ApiResponse<?> create(@Valid @RequestBody CategorySaveRequest request) { return ApiResponse.success(service.createCategory(request)); }
    @PutMapping("/{id}") public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody CategorySaveRequest request) { return ApiResponse.success(service.updateCategory(id, request)); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable Long id) { service.deleteCategory(id); return ApiResponse.success(null); }
    @PatchMapping("/{id}/status") public ApiResponse<Void> status(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) { service.updateCategoryStatus(id, request); return ApiResponse.success(null); }
    @GetMapping("/options") public ApiResponse<?> options(String keyword) { return ApiResponse.success(service.categoryOptions(keyword)); }
    @GetMapping("/export") public ResponseEntity<byte[]> export() { return export("categories.xlsx", excelExportService.exportEmptySheet("categories")); }

    private ResponseEntity<byte[]> export(String filename, byte[] bytes) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"").body(bytes);
    }
}
