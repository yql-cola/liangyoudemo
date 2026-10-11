package org.liangyou.modules.basic.service;

import org.liangyou.modules.basic.dto.ProductQueryRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class ExcelExportService {

    public byte[] exportProducts(ProductQueryRequest request) {
        return "productCode,productName\n".getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportEmptySheet(String resourceName) {
        return (resourceName + "\n").getBytes(StandardCharsets.UTF_8);
    }
}
