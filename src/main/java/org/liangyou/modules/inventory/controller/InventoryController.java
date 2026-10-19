package org.liangyou.modules.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.ApiResponse;
import org.liangyou.common.api.PageResponse;
import org.liangyou.modules.inventory.dto.InventoryFlowQueryRequest;
import org.liangyou.modules.inventory.dto.InventoryQueryRequest;
import org.liangyou.modules.inventory.dto.InventoryUpdateRequest;
import org.liangyou.modules.inventory.service.InventoryService;
import org.liangyou.modules.inventory.vo.InventoryBatchResponse;
import org.liangyou.modules.inventory.vo.InventoryFlowResponse;
import org.liangyou.modules.inventory.vo.InventoryResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Tag(name = "Inventory")
@RestController
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @Operation(summary = "分页查询库存汇总", description = "分页查询商品维度库存汇总。权限点：inventory:stock:list")
    public ApiResponse<PageResponse<InventoryResponse>> query(InventoryQueryRequest request) {
        return ApiResponse.success(inventoryService.query(request));
    }

    @GetMapping("/{productId}/batches")
    @Operation(summary = "查询库存批次明细", description = "按仓库和商品查看所有库存批次。权限点：inventory:batch:list")
    public ApiResponse<List<InventoryBatchResponse>> batches(@RequestParam(required = false) Long warehouseId,
                                                             @PathVariable Long productId) {
        return ApiResponse.success(inventoryService.batches(warehouseId, productId));
    }

    @GetMapping("/batches")
    @Operation(summary = "查询库存批次", description = "可按仓库、商品、分类筛选。权限点：inventory:batch:list")
    public ApiResponse<List<InventoryBatchResponse>> batches(InventoryQueryRequest request) {
        return ApiResponse.success(inventoryService.batches(request.getWarehouseId(), request.getProductId(), request.getCategoryId()));
    }

    @GetMapping("/export")
    @Operation(summary = "导出库存", description = "按当前筛选条件导出全部库存。权限点：inventory:stock:export")
    public ResponseEntity<byte[]> export(InventoryQueryRequest request) {
        return download("inventories", inventoryService.exportStocks(request));
    }

    @GetMapping("/batches/export")
    @Operation(summary = "导出库存批次", description = "按当前筛选条件导出全部批次。权限点：inventory:batch:export")
    public ResponseEntity<byte[]> exportBatches(InventoryQueryRequest request) {
        return download("inventory-batches", inventoryService.exportBatches(request));
    }

    @GetMapping("/flows/export")
    @Operation(summary = "导出库存流水", description = "按当前筛选条件导出全部流水。权限点：inventory:flow:export")
    public ResponseEntity<byte[]> exportFlows(InventoryFlowQueryRequest request) {
        return download("inventory-flows", inventoryService.exportFlows(request));
    }

    private ResponseEntity<byte[]> download(String name, byte[] bytes) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(bytes);
    }

    @GetMapping("/flows")
    @Operation(summary = "查询库存流水", description = "按时间范围查看库存变动流水。权限点：inventory:flow:list")
    public ApiResponse<List<InventoryFlowResponse>> flows(InventoryFlowQueryRequest request) {
        return ApiResponse.success(inventoryService.flows(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改库存汇总", description = "盘点修正同步库存汇总、批次和调整流水，保持锁定量不变。权限点：inventory:stock:update")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody InventoryUpdateRequest request) {
        inventoryService.update(id, request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除库存记录", description = "仅删除从未被使用且数量为零的库存记录。权限点：inventory:stock:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        inventoryService.delete(id);
        return ApiResponse.success(null);
    }
}
