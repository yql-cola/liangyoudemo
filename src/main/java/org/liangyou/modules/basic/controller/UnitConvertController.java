package org.liangyou.modules.basic.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.ApiResponse;
import org.liangyou.common.api.PageResponse;
import org.liangyou.modules.basic.dto.BasicQueryRequest;
import org.liangyou.modules.basic.dto.UnitConvertSaveRequest;
import org.liangyou.modules.basic.dto.StatusUpdateRequest;
import org.liangyou.modules.basic.entity.BaseProductUnitConvert;
import org.liangyou.modules.basic.vo.BasicOptionResponse;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.basic.service.ExcelExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Basic UnitConvert")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/basic/unit-converts")
@RequiredArgsConstructor
public class UnitConvertController {

    private final BasicDataService service;
    private final ExcelExportService excelExportService;

    @GetMapping
    @Operation(summary = "分页查询商品单位换算")
    public ApiResponse<PageResponse<BaseProductUnitConvert>> query(@Valid BasicQueryRequest request) { return ApiResponse.success(service.queryUnitConverts(request)); }

    @GetMapping("/{id}")
    @Operation(summary = "查询商品单位换算详情")
    public ApiResponse<BaseProductUnitConvert> detail(@PathVariable("id") Long id) { return ApiResponse.success(service.unitConvertDetail(id)); }

    @PostMapping
    @Operation(summary = "新增商品单位换算")
    public ApiResponse<BaseProductUnitConvert> create(@Valid @RequestBody UnitConvertSaveRequest request) { return ApiResponse.success(service.createUnitConvert(request)); }

    @PutMapping("/{id}")
    @Operation(summary = "修改商品单位换算")
    public ApiResponse<BaseProductUnitConvert> update(@PathVariable("id") Long id, @Valid @RequestBody UnitConvertSaveRequest request) { return ApiResponse.success(service.updateUnitConvert(id, request)); }

    @DeleteMapping("/{id}")
    @Operation(summary = "逻辑删除商品单位换算（已引用数据不可删除）")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) { service.deleteUnitConvert(id); return ApiResponse.success(null); }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启停用商品单位换算")
    public ApiResponse<Void> status(@PathVariable("id") Long id, @Valid @RequestBody StatusUpdateRequest request) { service.updateUnitConvertStatus(id, request); return ApiResponse.success(null); }

    @GetMapping("/options")
    @Operation(summary = "查询启用的商品单位换算选项")
    public ApiResponse<List<BasicOptionResponse>> options(BasicQueryRequest request) { return ApiResponse.success(service.unitConvertOptions(request)); }

    @GetMapping("/export")
    @Operation(summary = "导出全部符合筛选条件的商品单位换算")
    public ResponseEntity<byte[]> export(@Valid BasicQueryRequest request) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"unit-converts.xlsx\"")
                .body(excelExportService.exportBasic("unit-converts", request));
    }
}
