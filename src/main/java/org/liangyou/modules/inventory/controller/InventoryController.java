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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/batches")
    @Operation(summary = "查询库存批次明细", description = "按仓库和商品查看所有库存批次。权限点：inventory:batch:list")
    public ApiResponse<List<InventoryBatchResponse>> batches(Long warehouseId, Long productId) {
        return ApiResponse.success(inventoryService.batches(warehouseId, productId));
    }

    @GetMapping("/flows")
    @Operation(summary = "查询库存流水", description = "按时间范围查看库存变动流水。权限点：inventory:flow:list")
    public ApiResponse<List<InventoryFlowResponse>> flows(InventoryFlowQueryRequest request) {
        return ApiResponse.success(inventoryService.flows(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改库存汇总", description = "直接修正库存汇总并写入调整流水。权限点：inventory:stock:update")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody InventoryUpdateRequest request) {
        inventoryService.update(id, request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除库存记录", description = "逻辑删除库存汇总记录。权限点：inventory:stock:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        inventoryService.delete(id);
        return ApiResponse.success(null);
    }
}
