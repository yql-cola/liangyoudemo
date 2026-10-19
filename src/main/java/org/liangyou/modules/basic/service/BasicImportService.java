package org.liangyou.modules.basic.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.basic.dto.*;
import org.liangyou.modules.basic.entity.BaseProductCategory;
import org.liangyou.modules.basic.vo.BasicOptionResponse;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BasicImportService {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int MAX_ROWS = 2000;
    private static final Map<String, List<String>> HEADERS = Map.of(
            "products", List.of("商品编码", "商品名称", "分类", "品牌", "规格", "条码", "图片地址", "基础单位", "保质期天数", "状态", "备注"),
            "categories", List.of("分类名称", "父分类", "排序", "状态", "备注"),
            "units", List.of("单位编码", "单位名称", "状态", "备注"),
            "unit-converts", List.of("商品编码", "源单位编码", "目标单位编码", "换算比率", "状态", "备注"),
            "warehouses", List.of("仓库编码", "仓库名称", "联系人", "联系电话", "地址", "状态", "备注"),
            "suppliers", List.of("供应商编码", "供应商名称", "联系人", "联系电话", "地址", "税号", "开户行", "银行账号", "状态", "备注"),
            "customers", List.of("客户编码", "客户名称", "联系人", "联系电话", "地址", "税号", "开户行", "银行账号", "状态", "备注"));
    private static final Map<String, String> FIELD_LABELS = Map.ofEntries(
            Map.entry("productCode", "商品编码"), Map.entry("productName", "商品名称"),
            Map.entry("categoryId", "分类"), Map.entry("baseUnitId", "基础单位"),
            Map.entry("brandName", "品牌"), Map.entry("spec", "规格"), Map.entry("barcode", "条码"),
            Map.entry("imageUrl", "图片地址"), Map.entry("shelfLifeDays", "保质期天数"),
            Map.entry("categoryName", "分类名称"), Map.entry("parentId", "父分类"), Map.entry("sortNo", "排序"),
            Map.entry("unitCode", "单位编码"), Map.entry("unitName", "单位名称"),
            Map.entry("warehouseCode", "仓库编码"), Map.entry("warehouseName", "仓库名称"),
            Map.entry("supplierCode", "供应商编码"), Map.entry("supplierName", "供应商名称"),
            Map.entry("customerCode", "客户编码"), Map.entry("customerName", "客户名称"),
            Map.entry("productId", "商品编码"), Map.entry("fromUnitId", "源单位编码"),
            Map.entry("toUnitId", "目标单位编码"), Map.entry("convertRate", "换算比率"),
            Map.entry("contactName", "联系人"), Map.entry("contactPhone", "联系电话"),
            Map.entry("address", "地址"), Map.entry("taxNo", "税号"),
            Map.entry("bankName", "开户行"), Map.entry("bankAccount", "银行账号"),
            Map.entry("status", "状态"), Map.entry("remark", "备注"));
    private static final Map<String, List<String>> REQUIRED = Map.of(
            "products", List.of("商品编码", "商品名称", "分类", "基础单位"),
            "categories", List.of("分类名称"), "units", List.of("单位编码", "单位名称"),
            "unit-converts", List.of("商品编码", "源单位编码", "目标单位编码", "换算比率"),
            "warehouses", List.of("仓库编码", "仓库名称"),
            "suppliers", List.of("供应商编码", "供应商名称"),
            "customers", List.of("客户编码", "客户名称"));

    private final BasicDataService data;
    private final Validator validator;

    public record ImportError(int rowNumber, String columnName, String message) {}
    public record ImportResult(int totalRows, int successRows, int failedRows, List<ImportError> errors) {}

    public byte[] template(String resource) {
        List<String> headers = headers(resource);
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = book.createSheet("导入模板");
            Row row = sheet.createRow(0);
            // Set code-like columns to text so Excel preserves leading zeroes when users fill the template.
            var textStyle = book.createCellStyle();
            textStyle.setDataFormat(book.createDataFormat().getFormat("@"));
            for (int i = 0; i < headers.size(); i++)
                if (headers.get(i).contains("编码") || headers.get(i).equals("条码") || headers.get(i).equals("银行账号"))
                    sheet.setDefaultColumnStyle(i, textStyle);
            for (int i = 0; i < headers.size(); i++) row.createCell(i).setCellValue(headers.get(i));
            book.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("import-template-failed", ex);
        }
    }

    @Transactional
    public ImportResult importFile(String resource, MultipartFile file) {
        List<String> headers = headers(resource);
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx"))
            throw new BusinessException(400, "仅支持非空 XLSX 文件");
        if (file.getSize() > MAX_BYTES) throw new BusinessException(400, "文件不能超过 5 MB");
        List<SheetRow> rows = read(file, headers);
        log.info("basic import started resource={} rows={}", resource, rows.size());
        List<ImportError> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (SheetRow row : rows) {
            preflight(resource, row, errors);
            String key = duplicateKey(resource, row.values());
            if (!row.values().get(headers.get(0)).isBlank() && !seen.add(key))
                errors.add(new ImportError(row.number(), headers.get(0), "文件内记录重复"));
        }
        if (!errors.isEmpty()) {
            log.warn("basic import preflight rejected resource={} rows={} errors={}", resource, rows.size(), errors.size());
            return new ImportResult(rows.size(), 0, rows.size(), errors);
        }

        // Category parents must exist before children; all inserts share this transaction.
        if (resource.equals("categories")) rows.sort(Comparator.comparingInt(row -> depth(row.values().get("父分类"))));
        Map<String, Long> categories = Set.of("categories", "products").contains(resource)
                ? categoryPaths(data.categoryTree()) : new HashMap<>();
        for (SheetRow row : rows) {
            try {
                create(resource, row.values(), categories);
            } catch (RuntimeException ex) {
                log.warn("basic import rolled back resource={} row={} errorType={}", resource, row.number(), ex.getClass().getSimpleName());
                String column = ex instanceof ImportFieldException field ? field.column() : headers.get(0);
                throw new ImportRowException(rows.size(), row.number(), column, importMessage(ex), ex);
            }
        }
        log.info("basic import completed resource={} rows={}", resource, rows.size());
        return new ImportResult(rows.size(), rows.size(), 0, List.of());
    }

    public static class ImportRowException extends RuntimeException {
        private final ImportResult result;
        public ImportRowException(int total, int row, String column, String message, Throwable cause) {
            super(message, cause);
            result = new ImportResult(total, 0, total, List.of(new ImportError(row, column, message)));
        }
        public ImportResult result() { return result; }
    }

    private static class ImportFieldException extends RuntimeException {
        private final String column;
        ImportFieldException(String column, String message) { super(message); this.column = column; }
        String column() { return column; }
    }

    private String importMessage(RuntimeException ex) {
        if (ex instanceof ImportFieldException) return ex.getMessage();
        if (!(ex instanceof BusinessException)) return "资料写入失败，请联系管理员";
        String code = ex.getMessage();
        if (code == null) return "资料写入失败";
        return switch (code) {
            case "basic-data-duplicate", "unit-convert-exists" -> "编码或换算关系已存在";
            case "basic-data-disabled" -> "关联资料已停用";
            case "basic-data-not-found", "category-required" -> "关联资料不存在";
            case "category-parent-invalid" -> "父分类无效";
            case "category-cycle", "convert-cycle" -> "分类或换算关系不能形成循环";
            case "convert-rate-inconsistent" -> "换算比率与已有关系冲突";
            case "convert-units-invalid" -> "源单位和目标单位不能相同";
            default -> code.matches("[a-z0-9-]+") ? "资料校验失败：" + code : code;
        };
    }

    private record SheetRow(int number, Map<String, String> values) {}

    private void preflight(String resource, SheetRow row, List<ImportError> errors) {
        Map<String, String> values = row.values();
        for (String column : REQUIRED.get(resource))
            if (values.get(column).isBlank()) errors.add(new ImportError(row.number(), column, column + "不能为空"));
        for (Map.Entry<String, String> cell : values.entrySet()) {
            String column = cell.getKey();
            String content = cell.getValue();
            int max = column.equals("备注") || column.equals("地址") || column.equals("图片地址") ? 255
                    : Set.of("单位编码", "单位名称", "联系电话").contains(column) ? 32
                    : Set.of("商品名称", "分类名称", "仓库名称", "供应商名称", "客户名称", "规格", "开户行").contains(column) ? 128 : 64;
            if (content.length() > max) errors.add(new ImportError(row.number(), column, column + "超过 " + max + " 个字符"));
        }
        String status = values.get("状态");
        if (!status.isBlank() && !Set.of("启用", "停用").contains(status))
            errors.add(new ImportError(row.number(), "状态", "只能填写启用或停用"));
        for (String column : List.of("排序", "保质期天数")) {
            String raw = values.get(column);
            if (raw == null || raw.isBlank()) continue;
            try { if (Integer.parseInt(raw) < 0) throw new NumberFormatException(); }
            catch (NumberFormatException ex) { errors.add(new ImportError(row.number(), column, column + "必须是非负整数")); }
        }
        String rate = values.get("换算比率");
        if (rate != null && !rate.isBlank()) {
            try {
                BigDecimal amount = new BigDecimal(rate);
                if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.scale() > 6 || amount.precision() - amount.scale() > 12)
                    errors.add(new ImportError(row.number(), "换算比率", "换算比率必须为正数，最多六位小数"));
            } catch (NumberFormatException ex) { errors.add(new ImportError(row.number(), "换算比率", "换算比率必须是数字")); }
        }
    }

    private List<SheetRow> read(MultipartFile file, List<String> headers) {
        try (XSSFWorkbook book = new XSSFWorkbook(file.getInputStream())) {
            if (book.getNumberOfSheets() == 0 || book.getSheetAt(0).getPhysicalNumberOfRows() == 0)
                throw new BusinessException(400, "表格不能为空");
            var sheet = book.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(Locale.CHINA);
            Row head = sheet.getRow(0);
            for (int i = 0; i < headers.size(); i++)
                if (head == null || !headers.get(i).equals(value(head.getCell(i), formatter)))
                    throw new BusinessException(400, "表头或列顺序不符，请使用下载的导入模板");
            if (head.getLastCellNum() > headers.size()) throw new BusinessException(400, "表格含有模板外的列");
            List<SheetRow> rows = new ArrayList<>();
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row excelRow = sheet.getRow(index);
                if (excelRow == null) continue;
                Map<String, String> values = new LinkedHashMap<>();
                boolean filled = false;
                for (int col = 0; col < headers.size(); col++) {
                    String cell = value(excelRow.getCell(col), formatter);
                    values.put(headers.get(col), cell);
                    filled |= !cell.isBlank();
                }
                if (filled) rows.add(new SheetRow(index + 1, values));
                if (rows.size() > MAX_ROWS) throw new BusinessException(400, "单次最多导入 2000 行");
            }
            if (rows.isEmpty()) throw new BusinessException(400, "表格没有数据行");
            return rows;
        } catch (IOException | org.apache.poi.ooxml.POIXMLException ex) {
            throw new BusinessException(400, "XLSX 文件内容无效");
        }
    }

    private String value(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.FORMULA) throw new BusinessException(400, "导入文件不能包含公式");
        return formatter.formatCellValue(cell).trim();
    }

    private void create(String resource, Map<String, String> row, Map<String, Long> categories) {
        Integer status = status(row);
        switch (resource) {
            case "units" -> {
                UnitSaveRequest request = new UnitSaveRequest();
                request.setUnitCode(row.get("单位编码")); request.setUnitName(row.get("单位名称"));
                request.setStatus(status); request.setRemark(row.get("备注")); validate(request); data.createUnit(request);
            }
            case "categories" -> {
                CategorySaveRequest request = new CategorySaveRequest();
                String parent = row.get("父分类");
                request.setParentId(parent.isBlank() ? 0L : required(categories.get(parent), "父分类", "父分类不存在"));
                request.setCategoryName(row.get("分类名称")); request.setSortNo(integer(row.get("排序"), "排序"));
                request.setStatus(status); request.setRemark(row.get("备注")); validate(request);
                BaseProductCategory saved = data.createCategory(request);
                categories.put(parent.isBlank() ? request.getCategoryName() : parent + "/" + request.getCategoryName(), saved.getId());
            }
            case "products" -> {
                ProductSaveRequest request = new ProductSaveRequest();
                request.setProductCode(row.get("商品编码")); request.setProductName(row.get("商品名称"));
                request.setCategoryId(required(categories.get(row.get("分类")), "分类", "分类不存在"));
                request.setBaseUnitId(optionId(data.unitOptions(null), row.get("基础单位"), "基础单位"));
                request.setBrandName(row.get("品牌")); request.setSpec(row.get("规格"));
                request.setBarcode(row.get("条码")); request.setImageUrl(row.get("图片地址"));
                request.setShelfLifeDays(integer(row.get("保质期天数"), "保质期天数"));
                request.setStatus(status); request.setRemark(row.get("备注")); validate(request); data.createProduct(request);
            }
            case "unit-converts" -> {
                UnitConvertSaveRequest request = new UnitConvertSaveRequest();
                request.setProductId(optionId(data.productOptions(null), row.get("商品编码"), "商品编码"));
                List<BasicOptionResponse> units = data.unitOptions(null);
                request.setFromUnitId(optionId(units, row.get("源单位编码"), "源单位编码"));
                request.setToUnitId(optionId(units, row.get("目标单位编码"), "目标单位编码"));
                try { request.setConvertRate(new BigDecimal(row.get("换算比率"))); }
                catch (NumberFormatException ex) { throw new ImportFieldException("换算比率", "换算比率必须是数字"); }
                request.setStatus(status); request.setRemark(row.get("备注")); validate(request); data.createUnitConvert(request);
            }
            case "warehouses" -> {
                WarehouseSaveRequest request = new WarehouseSaveRequest();
                commonParty(request, row, "仓库"); validate(request); data.createWarehouse(request);
            }
            case "suppliers" -> {
                SupplierSaveRequest request = new SupplierSaveRequest();
                commonParty(request, row, "供应商"); validate(request); data.createSupplier(request);
            }
            case "customers" -> {
                CustomerSaveRequest request = new CustomerSaveRequest();
                commonParty(request, row, "客户"); validate(request); data.createCustomer(request);
            }
            default -> throw new BusinessException(400, "不支持的基础资料类型");
        }
    }

    private void commonParty(Object request, Map<String, String> row, String label) {
        BeanWrapperImpl bean = new BeanWrapperImpl(request);
        String prefix = switch (label) { case "仓库" -> "warehouse"; case "供应商" -> "supplier"; default -> "customer"; };
        bean.setPropertyValue(prefix + "Code", row.get(label + "编码"));
        bean.setPropertyValue(prefix + "Name", row.get(label + "名称"));
        for (String[] pair : List.of(new String[]{"联系人", "contactName"}, new String[]{"联系电话", "contactPhone"},
                new String[]{"地址", "address"}, new String[]{"税号", "taxNo"}, new String[]{"开户行", "bankName"}, new String[]{"银行账号", "bankAccount"}))
            if (bean.isWritableProperty(pair[1])) bean.setPropertyValue(pair[1], row.get(pair[0]));
        bean.setPropertyValue("status", status(row)); bean.setPropertyValue("remark", row.get("备注"));
    }

    private <T> void validate(T request) {
        Set<ConstraintViolation<T>> problems = validator.validate(request);
        if (!problems.isEmpty()) {
            ConstraintViolation<T> problem = problems.iterator().next();
            String label = FIELD_LABELS.getOrDefault(problem.getPropertyPath().toString(), problem.getPropertyPath().toString());
            String code = problem.getMessage();
            String reason = code.contains("required") ? "不能为空" : code.contains("too-long") ? "超过长度限制" : "格式或取值无效";
            throw new ImportFieldException(label, label + reason);
        }
    }

    private Integer status(Map<String, String> row) {
        return "停用".equals(row.get("状态")) ? 0 : 1;
    }

    private Integer integer(String raw, String column) {
        if (raw == null || raw.isBlank()) return null;
        try { return Integer.valueOf(raw); }
        catch (NumberFormatException ex) { throw new ImportFieldException(column, column + "必须是整数"); }
    }

    private Long optionId(List<BasicOptionResponse> options, String code, String column) {
        return options.stream().filter(option -> code.equals(option.getCode())).map(BasicOptionResponse::getId)
                .findFirst().orElseThrow(() -> new ImportFieldException(column, column + "不存在或已停用"));
    }

    private Long required(Long id, String column, String message) {
        if (id == null) throw new ImportFieldException(column, message);
        return id;
    }

    private int depth(String path) { return path == null || path.isBlank() ? 0 : path.split("/").length; }

    private Map<String, Long> categoryPaths(List<BaseProductCategory> roots) {
        Map<String, Long> result = new HashMap<>();
        for (BaseProductCategory root : roots) collectCategory(root, "", result);
        return result;
    }

    private void collectCategory(BaseProductCategory node, String parent, Map<String, Long> result) {
        String path = parent.isBlank() ? node.getCategoryName() : parent + "/" + node.getCategoryName();
        if (result.putIfAbsent(path, node.getId()) != null) throw new BusinessException(400, "存在重复分类路径：" + path);
        if (node.getChildren() != null) for (BaseProductCategory child : node.getChildren()) collectCategory(child, path, result);
    }

    private String duplicateKey(String resource, Map<String, String> row) {
        return switch (resource) {
            case "categories" -> row.get("父分类") + "/" + row.get("分类名称");
            case "unit-converts" -> row.get("商品编码") + "/" + row.get("源单位编码") + "/" + row.get("目标单位编码");
            default -> row.get(headers(resource).get(0));
        };
    }

    private List<String> headers(String resource) {
        List<String> result = HEADERS.get(resource);
        if (result == null) throw new BusinessException(400, "不支持的基础资料类型");
        return result;
    }
}
