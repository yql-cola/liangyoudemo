package org.liangyou.modules.sale.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.ApiResponse;
import org.liangyou.common.api.PageResponse;
import org.liangyou.modules.sale.dto.SaleOutCreateRequest;
import org.liangyou.modules.sale.dto.SaleOutQueryRequest;
import org.liangyou.modules.sale.service.SaleOutService;
import org.liangyou.modules.sale.vo.SaleOutDetailResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Sale Out")
@RestController
@RequestMapping("/api/v1/sale-outs")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class SaleOutController {

    private final SaleOutService saleOutService;

    @PostMapping
    @Operation(summary = "新增销售出库单", description = "新增销售单并按 FIFO 扣减库存批次。权限点：sale:order:create")
    public ApiResponse<Long> create(@Valid @RequestBody SaleOutCreateRequest request) {
        return ApiResponse.success(saleOutService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改销售出库单", description = "回滚旧出库影响后按新请求重建销售单和扣减明细。权限点：sale:order:update")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody SaleOutCreateRequest request) {
        saleOutService.update(id, request);
        return ApiResponse.success(null);
    }

    @GetMapping
    @Operation(summary = "分页查询销售出库单", description = "支持按单号、仓库、客户、业务日期和状态分页查询。权限点：sale:order:list")
    public ApiResponse<PageResponse<SaleOutDetailResponse>> query(SaleOutQueryRequest request) {
        return ApiResponse.success(saleOutService.query(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询销售出库单详情", description = "查询销售单主表和明细项。权限点：sale:order:view")
    public ApiResponse<SaleOutDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(saleOutService.detail(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除销售出库单", description = "逻辑删除销售出库单。权限点：sale:order:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        saleOutService.delete(id);
        return ApiResponse.success(null);
    }
}
