package org.liangyou.modules.basic.service;

import org.liangyou.modules.basic.dto.ProductQueryRequest;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelExportService {

    public byte[] exportProducts(ProductQueryRequest request) {
        return workbook("products", List.of("商品编码", "商品名称", "分类", "单位", "状态"));
    }

    public byte[] exportEmptySheet(String resourceName) {
        return workbook(resourceName, List.of("编码", "名称", "状态", "备注"));
    }

    private byte[] workbook(String sheetName, List<String> headers) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                header.createCell(i).setCellValue(headers.get(i));
                sheet.autoSizeColumn(i);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("export-failed", exception);
        }
    }
}
