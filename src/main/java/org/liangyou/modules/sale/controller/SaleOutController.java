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
import org.liangyou.modules.sale.service.SaleDocumentExportService;
import org.liangyou.modules.sale.vo.SaleOutDetailResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
import java.nio.charset.StandardCharsets;

@Tag(name = "Sale Out")
@RestController
@RequestMapping("/api/v1/sale-outs")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class SaleOutController {

    private final SaleOutService saleOutService;
    private final SaleDocumentExportService exports;

    @PostMapping
    @Operation(summary = "保存销售订单草稿", description = "保存草稿不占用也不扣减库存。权限点：sale:order:create")
    public ApiResponse<Long> create(@Valid @RequestBody SaleOutCreateRequest request) {
        return ApiResponse.success(saleOutService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改未完成销售订单", description = "确认状态下重新校验并占用库存。权限点：sale:order:update")
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

    @PostMapping("/{id}/confirm")
    public ApiResponse<Void> confirm(@PathVariable Long id) { saleOutService.confirm(id); return ApiResponse.success(null); }

    @PostMapping("/{id}/handoff")
    public ApiResponse<Void> handoff(@PathVariable Long id) { saleOutService.handoff(id); return ApiResponse.success(null); }

    @PostMapping("/{id}/start-delivery")
    public ApiResponse<Void> startDelivery(@PathVariable Long id) { saleOutService.startDelivery(id); return ApiResponse.success(null); }

    @PostMapping("/{id}/complete-delivery")
    public ApiResponse<Void> completeDelivery(@PathVariable Long id) { saleOutService.completeDelivery(id); return ApiResponse.success(null); }

    @PostMapping("/{id}/delivery-failed")
    public ApiResponse<Void> deliveryFailed(@PathVariable Long id, @RequestBody java.util.Map<String,String> body) {
        saleOutService.deliveryFailed(id, body.get("reason")); return ApiResponse.success(null);
    }

    @PostMapping("/{id}/return")
    public ApiResponse<Void> returned(@PathVariable Long id) { saleOutService.returned(id); return ApiResponse.success(null); }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) { saleOutService.cancel(id); return ApiResponse.success(null); }

    @GetMapping("/{id}/export/excel")
    public ResponseEntity<byte[]> excel(@PathVariable Long id) {
        return download(id, exports.excel(id), "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping("/{id}/export/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        return download(id, exports.pdf(id), "pdf", "application/pdf");
    }

    @GetMapping("/{id}/delivery-pdf")
    public ResponseEntity<byte[]> deliveryPdf(@PathVariable Long id) {
        return download(id, exports.deliveryPdf(id), "pdf", "application/pdf");
    }

    @PostMapping("/{id}/print")
    public ApiResponse<Void> print(@PathVariable Long id) {
        var order = exports.exportable(id);
        if (!"delivery".equals(order.getFulfillmentType())) throw new org.liangyou.common.exception.BusinessException(409, "sale-order-not-delivery");
        saleOutService.recordDocumentRequest(id, "print_requested");
        return ApiResponse.success(null);
    }

    private ResponseEntity<byte[]> download(Long id, byte[] bytes, String extension, String mediaType) {
        String filename = "销售清单-" + saleOutService.detail(id).getOrderNo() + "." + extension;
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(mediaType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(bytes);
    }
}
