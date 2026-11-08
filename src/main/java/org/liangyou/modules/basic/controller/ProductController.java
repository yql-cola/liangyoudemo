package org.liangyou.modules.basic.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.ApiResponse;
import org.liangyou.common.api.PageResponse;
import org.liangyou.modules.basic.dto.ProductQueryRequest;
import org.liangyou.modules.basic.dto.ProductSaveRequest;
import org.liangyou.modules.basic.dto.StatusUpdateRequest;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.basic.service.ExcelExportService;
import org.liangyou.modules.basic.service.ProductImageService;
import org.liangyou.modules.basic.vo.BasicOptionResponse;
import org.liangyou.modules.basic.vo.ProductResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.CacheControl;
import java.time.Duration;

import java.util.List;

@Tag(name = "Basic Product")
@RestController
@RequestMapping("/api/v1/basic/products")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class ProductController {

    private final BasicDataService basicDataService;
    private final ExcelExportService excelExportService;
    private final ProductImageService productImageService;

    @GetMapping
    @Operation(summary = "分页查询商品", description = "权限点：basic:product:list")
    public ApiResponse<PageResponse<ProductResponse>> query(@Valid ProductQueryRequest request) {
        return ApiResponse.success(basicDataService.queryProducts(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询商品详情", description = "权限点：basic:product:view")
    public ApiResponse<ProductResponse> detail(@PathVariable("id") Long id) {
        return ApiResponse.success(basicDataService.productDetail(id));
    }

    @PostMapping
    @Operation(summary = "新增商品", description = "权限点：basic:product:create")
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductSaveRequest request) {
        return ApiResponse.success(basicDataService.createProduct(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改商品", description = "权限点：basic:product:update")
    public ApiResponse<ProductResponse> update(@PathVariable("id") Long id, @Valid @RequestBody ProductSaveRequest request) {
        return ApiResponse.success(basicDataService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除商品", description = "权限点：basic:product:delete")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        basicDataService.deleteProduct(id);
        return ApiResponse.success(null);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启停用商品", description = "权限点：basic:product:status")
    public ApiResponse<Void> updateStatus(@PathVariable("id") Long id, @Valid @RequestBody StatusUpdateRequest request) {
        basicDataService.updateProductStatus(id, request);
        return ApiResponse.success(null);
    }

    @GetMapping("/options")
    @Operation(summary = "查询商品下拉选项")
    public ApiResponse<List<BasicOptionResponse>> options(String keyword) {
        return ApiResponse.success(basicDataService.productOptions(keyword));
    }

    @GetMapping("/{id}/units")
    @Operation(summary = "查询商品可用单位及到基础单位的换算倍率")
    public ApiResponse<List<BasicOptionResponse>> units(@PathVariable("id") Long id) {
        return ApiResponse.success(basicDataService.productUnitOptions(id));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传或替换商品主图", description = "权限点：basic:product:update")
    public ApiResponse<Void> uploadImage(@PathVariable("id") Long id, @RequestParam("file") MultipartFile file) {
        productImageService.upload(id, file);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}/image")
    @Operation(summary = "删除商品主图", description = "权限点：basic:product:update")
    public ApiResponse<Void> deleteImage(@PathVariable("id") Long id) {
        productImageService.delete(id);
        return ApiResponse.success(null);
    }

    @GetMapping(value = "/{id}/image")
    @Operation(summary = "读取商品主图")
    public ResponseEntity<InputStreamResource> image(@PathVariable("id") Long id) { return imageResponse(id, false); }

    @GetMapping(value = "/{id}/thumbnail")
    @Operation(summary = "读取商品缩略图")
    public ResponseEntity<InputStreamResource> thumbnail(@PathVariable("id") Long id) { return imageResponse(id, true); }

    private ResponseEntity<InputStreamResource> imageResponse(Long id, boolean thumbnail) {
        ProductImageService.ImageContent image = productImageService.open(id, thumbnail);
        // Write and close the storage stream in the authenticated request, without async redispatch.
        InputStreamResource body = new InputStreamResource(image.stream());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePrivate()).body(body);
    }

    @GetMapping("/export")
    @Operation(summary = "导出商品", description = "权限点：basic:product:export")
    public ResponseEntity<byte[]> export(@Valid ProductQueryRequest request) {
        byte[] bytes = excelExportService.exportProducts(request);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("products.xlsx").build().toString())
                .body(bytes);
    }
}
