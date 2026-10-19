package org.liangyou.modules.basic.service;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.liangyou.modules.basic.dto.ProductQueryRequest;

import java.io.ByteArrayInputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExcelExportServiceTest {

    @Test
    void productExportWritesActualFilteredRowsWithStableColumnOrder() throws Exception {
        BasicDataService data = mock(BasicDataService.class);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("商品编码", "0001"); row.put("商品名称", "东北大米");
        when(data.exportProducts(any())).thenReturn(List.of(row));
        ProductQueryRequest query = new ProductQueryRequest(); query.setKeyword("大米");
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(new ExcelExportService(data).exportProducts(query)))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals("商品编码", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("0001", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("东北大米", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals(1, sheet.getLastRowNum());
        }
        verify(data).exportProducts(query);
    }

    @Test
    void genericExportNeutralizesFormulaTokensAndKeepsOrderedColumns() throws Exception {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("编码", "=HYPERLINK(\"https://example.invalid\")");
        row.put("名称", "+cmd");
        row.put("备注", "@SUM(A1)");
        byte[] bytes = new ExcelExportService().export("report/unsafe", List.of(row));
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals("编码", sheet.getRow(0).getCell(0).getStringCellValue());
            assertTrue(sheet.getRow(1).getCell(0).getStringCellValue().startsWith("'="));
            assertEquals("'+cmd", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("'@SUM(A1)", sheet.getRow(1).getCell(2).getStringCellValue());
        }
    }

    @Test
    void emptyProductExportStillContainsColumnHeaders() throws Exception {
        BasicDataService data = mock(BasicDataService.class);
        when(data.exportProducts(any())).thenReturn(List.of());
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(new ExcelExportService(data).exportProducts(new ProductQueryRequest())))) {
            assertEquals("商品编码", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals(0, workbook.getSheetAt(0).getLastRowNum());
        }
    }
}
