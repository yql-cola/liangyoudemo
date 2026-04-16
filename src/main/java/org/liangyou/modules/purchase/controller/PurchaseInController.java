package org.liangyou.modules.purchase.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.ApiResponse;
import org.liangyou.common.api.PageResponse;
import org.liangyou.modules.purchase.dto.PurchaseInCreateRequest;
import org.liangyou.modules.purchase.dto.PurchaseInQueryRequest;
import org.liangyou.modules.purchase.service.PurchaseInService;
import org.liangyou.modules.purchase.vo.PurchaseInDetailResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Purchase In")
@RestController
@RequestMapping("/api/v1/purchase-ins")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class PurchaseInController {

    private final PurchaseInService purchaseInService;

    @PostMapping
    @Operation(summary = "新增采购入库单", description = "新增采购单并同步增加库存、批次和库存流水。权限点：purchase:order:create")
    public ApiResponse<Long> create(@Valid @RequestBody PurchaseInCreateRequest request) {
        return ApiResponse.success(purchaseInService.create(request));
    }

    @GetMapping
    @Operation(summary = "分页查询采购入库单", description = "支持按单号、仓库、供应商、业务日期和状态分页查询。权限点：purchase:order:list")
    public ApiResponse<PageResponse<PurchaseInDetailResponse>> query(PurchaseInQueryRequest request) {
        return ApiResponse.success(purchaseInService.query(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询采购入库单详情", description = "查询采购单主表和明细项。权限点：purchase:order:view")
    public ApiResponse<PurchaseInDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(purchaseInService.detail(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除采购入库单", description = "逻辑删除采购入库单。权限点：purchase:order:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        purchaseInService.delete(id);
        return ApiResponse.success(null);
    }
}
