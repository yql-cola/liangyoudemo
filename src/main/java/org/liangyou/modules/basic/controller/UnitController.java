package org.liangyou.modules.basic.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.ApiResponse;
import org.liangyou.common.api.PageResponse;
import org.liangyou.modules.basic.dto.BasicQueryRequest;
import org.liangyou.modules.basic.dto.UnitSaveRequest;
import org.liangyou.modules.basic.dto.StatusUpdateRequest;
import org.liangyou.modules.basic.entity.BaseUnit;
import org.liangyou.modules.basic.vo.BasicOptionResponse;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.basic.service.ExcelExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Basic Unit")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/basic/units")
@RequiredArgsConstructor
public class UnitController {

    private final BasicDataService service;
    private final ExcelExportService excelExportService;

    @GetMapping
    @Operation(summary = "分页查询计量单位")
    public ApiResponse<PageResponse<BaseUnit>> query(@Valid BasicQueryRequest request) { return ApiResponse.success(service.queryUnits(request)); }

    @GetMapping("/{id}")
    @Operation(summary = "查询计量单位详情")
    public ApiResponse<BaseUnit> detail(@PathVariable("id") Long id) { return ApiResponse.success(service.unitDetail(id)); }

    @PostMapping
    @Operation(summary = "新增计量单位")
    public ApiResponse<BaseUnit> create(@Valid @RequestBody UnitSaveRequest request) { return ApiResponse.success(service.createUnit(request)); }

    @PutMapping("/{id}")
    @Operation(summary = "修改计量单位")
    public ApiResponse<BaseUnit> update(@PathVariable("id") Long id, @Valid @RequestBody UnitSaveRequest request) { return ApiResponse.success(service.updateUnit(id, request)); }

    @DeleteMapping("/{id}")
    @Operation(summary = "逻辑删除计量单位（已引用数据不可删除）")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) { service.deleteUnit(id); return ApiResponse.success(null); }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启停用计量单位")
    public ApiResponse<Void> status(@PathVariable("id") Long id, @Valid @RequestBody StatusUpdateRequest request) { service.updateUnitStatus(id, request); return ApiResponse.success(null); }

    @GetMapping("/options")
    @Operation(summary = "查询启用的计量单位选项")
    public ApiResponse<List<BasicOptionResponse>> options(String keyword) { return ApiResponse.success(service.unitOptions(keyword)); }

    @GetMapping("/export")
    @Operation(summary = "导出全部符合筛选条件的计量单位")
    public ResponseEntity<byte[]> export(@Valid BasicQueryRequest request) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"units.xlsx\"")
                .body(excelExportService.exportBasic("units", request));
    }
}
