package org.liangyou.modules.basic.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.liangyou.common.api.ApiResponse;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.basic.dto.BasicQueryRequest;
import org.liangyou.modules.basic.dto.ProductQueryRequest;
import org.liangyou.modules.basic.service.BasicImportService;
import org.liangyou.modules.basic.service.ExcelExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/basic/{resource}")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Slf4j
public class BasicBulkController {
    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final Set<String> RESOURCES = Set.of("products", "categories", "units", "unit-converts", "warehouses", "suppliers", "customers");
    private final BasicImportService importer;
    private final ExcelExportService exporter;

    public record SelectedExportRequest(List<Long> ids) {}

    @GetMapping("/import/template")
    @Operation(summary = "下载基础资料导入模板")
    public ResponseEntity<byte[]> template(@PathVariable String resource) {
        checkResource(resource);
        return download(resource + "-import-template.xlsx", importer.template(resource));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "批量导入基础资料")
    public ApiResponse<BasicImportService.ImportResult> importData(@PathVariable String resource, @RequestParam("file") MultipartFile file) {
        checkResource(resource);
        return ApiResponse.success(importer.importFile(resource, file));
    }

    @PostMapping(value = "/export", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "导出勾选的基础资料")
    public ResponseEntity<byte[]> selectedExport(@PathVariable String resource, @RequestBody SelectedExportRequest request) {
        checkResource(resource);
        List<Long> ids = request == null ? null : request.ids();
        if (ids == null || ids.isEmpty() || ids.size() > 2000 || ids.stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(ids).size() != ids.size())
            throw new BusinessException(400, "导出选择无效或超过 2000 条");
        log.info("basic selected export resource={} count={}", resource, ids.size());
        byte[] bytes;
        if ("products".equals(resource)) {
            ProductQueryRequest query = new ProductQueryRequest(); query.setIds(ids);
            bytes = exporter.exportProducts(query);
        } else {
            BasicQueryRequest query = new BasicQueryRequest(); query.setIds(ids);
            bytes = exporter.exportBasic(resource, query);
        }
        return download(resource + ".xlsx", bytes);
    }

    private void checkResource(String resource) {
        if (!RESOURCES.contains(resource)) throw new BusinessException(400, "不支持的基础资料类型");
    }

    private ResponseEntity<byte[]> download(String filename, byte[] bytes) {
        return ResponseEntity.ok().contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(bytes);
    }
}
