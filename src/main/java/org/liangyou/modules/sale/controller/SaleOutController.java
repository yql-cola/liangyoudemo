package org.liangyou.modules.sale.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Sale Out")
@RestController
@RequestMapping("/api/v1/sale-outs")
@RequiredArgsConstructor
public class SaleOutController {

    private final SaleOutService saleOutService;

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody SaleOutCreateRequest request) {
        return ApiResponse.success(saleOutService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody SaleOutCreateRequest request) {
        saleOutService.update(id, request);
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<PageResponse<SaleOutDetailResponse>> query(SaleOutQueryRequest request) {
        return ApiResponse.success(saleOutService.query(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<SaleOutDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(saleOutService.detail(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        saleOutService.delete(id);
        return ApiResponse.success(null);
    }
}
