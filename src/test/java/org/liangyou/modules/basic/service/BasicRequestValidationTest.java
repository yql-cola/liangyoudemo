package org.liangyou.modules.basic.service;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.liangyou.modules.basic.dto.*;

import static org.junit.jupiter.api.Assertions.*;

class BasicRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void productRejectsTextBeyondDatabaseColumnLength() {
        ProductSaveRequest request = new ProductSaveRequest();
        request.setProductCode("X".repeat(65)); request.setProductName("大米");
        request.setCategoryId(1L); request.setBaseUnitId(1L);
        assertTrue(validator.validate(request).stream().anyMatch(v -> v.getPropertyPath().toString().equals("productCode")));
    }

    @Test
    void unitConversionRequiresPositiveUnitIdentifiersAndDatabaseScale() {
        UnitConvertSaveRequest request = new UnitConvertSaveRequest();
        request.setProductId(0L); request.setFromUnitId(-1L); request.setToUnitId(1L);
        request.setConvertRate(new java.math.BigDecimal("1.1234567"));
        var violations = validator.validate(request);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("productId")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("convertRate")));
    }

    @Test
    void everySaveDtoRejectsUnknownStatus() {
        for (Object request : new Object[]{new ProductSaveRequest(), new CategorySaveRequest(), new UnitSaveRequest(),
                new WarehouseSaveRequest(), new SupplierSaveRequest(), new CustomerSaveRequest(), new UnitConvertSaveRequest()}) {
            new org.springframework.beans.BeanWrapperImpl(request).setPropertyValue("status", 2);
            assertTrue(validator.validate(request).stream().anyMatch(v -> v.getPropertyPath().toString().equals("status")), request.getClass().getSimpleName());
        }
    }

    @Test
    void paginationIsBoundedEvenWithoutControllerValidation() {
        BasicQueryRequest request = new BasicQueryRequest();
        assertEquals(1, request.getPageNum()); assertEquals(10, request.getPageSize());
        request.setPageNum(-5); request.setPageSize(1000);
        assertEquals(1, request.getPageNum()); assertEquals(100, request.getPageSize());
        ProductQueryRequest products = new ProductQueryRequest(); products.setPageSize(101);
        assertEquals(100, products.getPageSize());
    }
}
