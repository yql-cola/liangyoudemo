package org.liangyou.modules.inventory.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Inventory")
@RestController
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ApiResponse<PageResponse<InventoryResponse>> query(InventoryQueryRequest request) {
        return ApiResponse.success(inventoryService.query(request));
    }

    @GetMapping("/batches")
    public ApiResponse<List<InventoryBatchResponse>> batches(Long warehouseId, Long productId) {
        return ApiResponse.success(inventoryService.batches(warehouseId, productId));
    }

    @GetMapping("/flows")
    public ApiResponse<List<InventoryFlowResponse>> flows(InventoryFlowQueryRequest request) {
        return ApiResponse.success(inventoryService.flows(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody InventoryUpdateRequest request) {
        inventoryService.update(id, request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        inventoryService.delete(id);
        return ApiResponse.success(null);
    }
}
