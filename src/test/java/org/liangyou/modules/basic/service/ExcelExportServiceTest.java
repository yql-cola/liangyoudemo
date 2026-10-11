package org.liangyou.modules.basic.service;

import org.junit.jupiter.api.Test;
import org.liangyou.modules.basic.dto.ProductQueryRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelExportServiceTest {

    @Test
    void productExportReturnsXlsxBytes() {
        ExcelExportService service = new ExcelExportService();

        byte[] bytes = service.exportProducts(new ProductQueryRequest());

        assertTrue(bytes.length > 10);
        assertEquals('P', bytes[0]);
        assertEquals('K', bytes[1]);
    }
}
