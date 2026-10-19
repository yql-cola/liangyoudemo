package org.liangyou.modules.basic.service;

import org.liangyou.modules.basic.dto.BasicQueryRequest;
import org.liangyou.modules.basic.dto.ProductQueryRequest;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class ExcelExportService {

    private final BasicDataService basicDataService;

    @Autowired
    public ExcelExportService(BasicDataService basicDataService) { this.basicDataService = basicDataService; }

    public ExcelExportService() { this(null); }

    public byte[] exportProducts(ProductQueryRequest request) {
        return export("products", basicDataService == null ? List.of() : basicDataService.exportProducts(request),
                List.of("商品编码", "商品名称", "分类", "品牌", "规格", "条码", "基础单位", "保质期天数", "状态", "备注", "创建时间", "更新时间"));
    }

    public byte[] exportBasic(String resourceName, BasicQueryRequest request) {
        return export(resourceName, basicDataService.exportBasic(resourceName, request), resourceHeaders(resourceName));
    }

    /** Kept for compatibility; resource controllers export filtered records through exportBasic. */
    public byte[] exportEmptySheet(String resourceName) { return export(resourceName, List.of()); }

    /** LinkedHashMap keys determine column order; all matching rows are written. */
    public byte[] export(String sheetName, List<Map<String, Object>> rows) {
        return export(sheetName, rows, List.of());
    }

    private byte[] export(String sheetName, List<Map<String, Object>> rows, List<String> emptyHeaders) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(org.apache.poi.ss.util.WorkbookUtil.createSafeSheetName(sheetName));
            List<String> columns = new ArrayList<>();
            for (Map<String, Object> row : rows) for (String key : row.keySet()) if (!columns.contains(key)) columns.add(key);
            if (columns.isEmpty()) columns.addAll(emptyHeaders);
            int[] widths = new int[columns.size()];
            Row header = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                header.createCell(i).setCellValue(columns.get(i));
                widths[i] = displayWidth(columns.get(i));
            }
            int rowNumber = 1;
            for (Map<String, Object> data : rows) {
                Row row = sheet.createRow(rowNumber++);
                for (int i = 0; i < columns.size(); i++) {
                    Object value = data.get(columns.get(i));
                    if (value == null) continue;
                    var cell = row.createCell(i);
                    // Decimal values stay exact, account numbers and codes keep their leading zeroes.
                    String text = String.valueOf(value);
                    String trimmed = text.stripLeading();
                    if (!(value instanceof Number) && ((!text.isEmpty() && "=+-@\t\r\n".indexOf(text.charAt(0)) >= 0)
                            || (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0))) text = "'" + text;
                    cell.setCellValue(text);
                    widths[i] = Math.max(widths[i], displayWidth(text));
                }
            }
            // Avoid AWT/font APIs: exports also run on headless servers.
            for (int i = 0; i < columns.size(); i++) sheet.setColumnWidth(i, Math.min(18000, Math.max(3000, (widths[i] + 2) * 256)));
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) { throw new IllegalStateException("export-failed", exception); }
    }
    private int displayWidth(String value) {
        return value.codePoints().map(code -> code > 255 ? 2 : 1).sum();
    }

    private List<String> resourceHeaders(String resource) {
        return switch (resource) {
            case "categories" -> List.of("分类名称", "父分类", "排序", "状态", "备注");
            case "units" -> List.of("单位编码", "单位名称", "状态", "备注");
            case "warehouses" -> List.of("仓库编码", "仓库名称", "联系人", "联系电话", "地址", "状态", "备注");
            case "suppliers" -> List.of("供应商编码", "供应商名称", "联系人", "联系电话", "地址", "税号", "开户行", "银行账号", "状态", "备注");
            case "customers" -> List.of("客户编码", "客户名称", "联系人", "联系电话", "地址", "税号", "开户行", "银行账号", "状态", "备注");
            case "unit-converts" -> List.of("商品名称", "源单位", "目标单位", "换算比例", "状态", "备注");
            default -> List.of();
        };
    }

}
