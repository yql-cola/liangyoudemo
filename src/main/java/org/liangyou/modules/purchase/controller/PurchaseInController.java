package org.liangyou.modules.purchase.controller;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Purchase In")
@RestController
@RequestMapping("/api/v1/purchase-ins")
@RequiredArgsConstructor
public class PurchaseInController {

    private final PurchaseInService purchaseInService;

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody PurchaseInCreateRequest request) {
        return ApiResponse.success(purchaseInService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody PurchaseInCreateRequest request) {
        purchaseInService.update(id, request);
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<PageResponse<PurchaseInDetailResponse>> query(PurchaseInQueryRequest request) {
        return ApiResponse.success(purchaseInService.query(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<PurchaseInDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(purchaseInService.detail(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        purchaseInService.delete(id);
        return ApiResponse.success(null);
    }
}
