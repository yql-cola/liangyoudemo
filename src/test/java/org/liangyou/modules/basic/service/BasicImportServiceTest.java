package org.liangyou.modules.basic.service;

import jakarta.validation.Validation;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BasicImportServiceTest {
    private final BasicDataService data = mock(BasicDataService.class);
    private final BasicImportService service = new BasicImportService(data, Validation.buildDefaultValidatorFactory().getValidator());

    @Test void templateHasChineseColumns() throws Exception {
        try (XSSFWorkbook book = new XSSFWorkbook(new java.io.ByteArrayInputStream(service.template("units")))) {
            assertEquals("单位编码", book.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("状态", book.getSheetAt(0).getRow(0).getCell(2).getStringCellValue());
        }
    }

    @Test void duplicateCodesInOneFileRejectEntireBatchBeforeWriting() throws Exception {
        var result = service.importFile("units", file("单位编码", "单位名称", "状态", "备注",
                List.of("U01", "袋", "启用", ""), List.of("U01", "箱", "启用", "")));
        assertEquals(2, result.totalRows());
        assertEquals(0, result.successRows());
        assertTrue(result.errors().stream().anyMatch(error -> error.rowNumber() == 3));
        verify(data, never()).createUnit(any());
    }

    @Test void validUnitRowsUseExistingCreateRules() throws Exception {
        var result = service.importFile("units", file("单位编码", "单位名称", "状态", "备注",
                List.of("U01", "袋", "启用", "")));
        assertEquals(1, result.successRows());
        verify(data).createUnit(argThat(request -> "U01".equals(request.getUnitCode()) && request.getStatus() == 1));
    }

    @Test void preflightCollectsErrorsFromDifferentRowsWithoutWriting() throws Exception {
        var result = service.importFile("units", file("单位编码", "单位名称", "状态", "备注",
                List.of("", "袋", "启用", ""), List.of("U02", "", "停用", "")));
        assertEquals(0, result.successRows());
        assertEquals(List.of(2, 3), result.errors().stream().map(BasicImportService.ImportError::rowNumber).toList());
        verify(data, never()).createUnit(any());
    }

    @SafeVarargs
    private static MockMultipartFile file(String a, String b, String c, String d, List<String>... rows) throws Exception {
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = book.createSheet();
            var header = sheet.createRow(0);
            String[] names = {a, b, c, d};
            for (int i = 0; i < names.length; i++) header.createCell(i).setCellValue(names[i]);
            for (int r = 0; r < rows.length; r++) {
                var row = sheet.createRow(r + 1);
                for (int i = 0; i < rows[r].size(); i++) row.createCell(i).setCellValue(rows[r].get(i));
            }
            book.write(out);
            return new MockMultipartFile("file", "units.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }
}
