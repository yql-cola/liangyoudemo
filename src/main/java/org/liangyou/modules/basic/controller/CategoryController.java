package org.liangyou.modules.basic.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.ApiResponse;
import org.liangyou.common.api.PageResponse;
import org.liangyou.modules.basic.dto.BasicQueryRequest;
import org.liangyou.modules.basic.dto.CategorySaveRequest;
import org.liangyou.modules.basic.dto.StatusUpdateRequest;
import org.liangyou.modules.basic.entity.BaseProductCategory;
import org.liangyou.modules.basic.vo.BasicOptionResponse;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.basic.service.ExcelExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Basic Category")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/basic/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final BasicDataService service;
    private final ExcelExportService excelExportService;

    @GetMapping
    @Operation(summary = "分页查询商品分类")
    public ApiResponse<PageResponse<BaseProductCategory>> query(@Valid BasicQueryRequest request) { return ApiResponse.success(service.queryCategories(request)); }

    @GetMapping("/{id}")
    @Operation(summary = "查询商品分类详情")
    public ApiResponse<BaseProductCategory> detail(@PathVariable("id") Long id) { return ApiResponse.success(service.categoryDetail(id)); }

    @PostMapping
    @Operation(summary = "新增商品分类")
    public ApiResponse<BaseProductCategory> create(@Valid @RequestBody CategorySaveRequest request) { return ApiResponse.success(service.createCategory(request)); }

    @PutMapping("/{id}")
    @Operation(summary = "修改商品分类")
    public ApiResponse<BaseProductCategory> update(@PathVariable("id") Long id, @Valid @RequestBody CategorySaveRequest request) { return ApiResponse.success(service.updateCategory(id, request)); }

    @DeleteMapping("/{id}")
    @Operation(summary = "逻辑删除商品分类（已引用数据不可删除）")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) { service.deleteCategory(id); return ApiResponse.success(null); }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启停用商品分类")
    public ApiResponse<Void> status(@PathVariable("id") Long id, @Valid @RequestBody StatusUpdateRequest request) { service.updateCategoryStatus(id, request); return ApiResponse.success(null); }

    @GetMapping("/options")
    @Operation(summary = "查询启用的商品分类选项")
    public ApiResponse<List<BasicOptionResponse>> options(String keyword) { return ApiResponse.success(service.categoryOptions(keyword)); }

    @GetMapping("/export")
    @Operation(summary = "导出全部符合筛选条件的商品分类")
    public ResponseEntity<byte[]> export(@Valid BasicQueryRequest request) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"categories.xlsx\"")
                .body(excelExportService.exportBasic("categories", request));
    }

    @GetMapping("/tree")
    @Operation(summary = "查询分类树")
    public ApiResponse<List<BaseProductCategory>> tree() { return ApiResponse.success(service.categoryTree()); }
}
