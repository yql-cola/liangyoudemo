package org.liangyou.modules.basic.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.basic.mapper.BaseCustomerMapper;
import org.liangyou.modules.basic.mapper.BaseProductCategoryMapper;
import org.liangyou.modules.basic.mapper.BaseProductUnitConvertMapper;
import org.liangyou.modules.basic.mapper.BaseSupplierMapper;
import org.liangyou.modules.basic.mapper.BaseUnitMapper;
import org.liangyou.modules.basic.mapper.BaseWarehouseMapper;
import org.liangyou.modules.basic.entity.BaseCustomer;
import org.liangyou.modules.basic.entity.BaseSupplier;
import org.liangyou.modules.basic.entity.BaseUnit;
import org.liangyou.modules.basic.entity.BaseWarehouse;
import org.liangyou.modules.basic.vo.BasicOptionResponse;
import org.liangyou.modules.basic.dto.*;
import org.liangyou.modules.basic.entity.BaseProductCategory;
import org.liangyou.modules.basic.entity.BaseProductUnitConvert;
import java.math.BigDecimal;
import org.liangyou.modules.inventory.mapper.InvBatchMapper;
import org.liangyou.modules.inventory.mapper.InvStockFlowMapper;
import org.liangyou.modules.inventory.mapper.InvStockMapper;
import org.liangyou.modules.product.entity.BaseProduct;
import org.liangyou.modules.product.mapper.BaseProductMapper;
import org.liangyou.modules.purchase.mapper.BizPurchaseInItemMapper;
import org.liangyou.modules.purchase.mapper.BizPurchaseInMapper;
import org.liangyou.modules.sale.mapper.BizSaleOutItemMapper;
import org.liangyou.modules.sale.mapper.BizSaleOutMapper;
import org.liangyou.modules.user.mapper.SysUserMapper;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasicDataServiceTest {

    @Mock
    private BaseProductMapper baseProductMapper;
    @Mock
    private BaseProductCategoryMapper categoryMapper;
    @Mock
    private BaseUnitMapper unitMapper;
    @Mock
    private BaseProductUnitConvertMapper unitConvertMapper;
    @Mock
    private BaseWarehouseMapper warehouseMapper;
    @Mock
    private BaseSupplierMapper supplierMapper;
    @Mock
    private BaseCustomerMapper customerMapper;
    @Mock
    private BizPurchaseInItemMapper purchaseInItemMapper;
    @Mock
    private BizSaleOutItemMapper saleOutItemMapper;
    @Mock
    private InvStockMapper invStockMapper;
    @Mock
    private InvBatchMapper invBatchMapper;
    @Mock
    private InvStockFlowMapper invStockFlowMapper;
    @Mock
    private BizPurchaseInMapper purchaseInMapper;
    @Mock
    private BizSaleOutMapper saleOutMapper;
    @Mock
    private SysUserMapper sysUserMapper;

    @Test
    void deleteProductRejectsReferencedProduct() {
        BasicDataService service = service();
        BaseProduct product = new BaseProduct();
        product.setId(10L);
        when(baseProductMapper.selectById(10L)).thenReturn(product);
        when(purchaseInItemMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.deleteProduct(10L));

        assertEquals(400, exception.getCode());
        assertEquals("basic-data-in-use", exception.getMessage());
        verify(baseProductMapper, never()).deleteById(10L);
    }

    @Test
    void productOptionsOnlyReturnEnabledProducts() {
        BasicDataService service = service();
        BaseProduct product = new BaseProduct();
        product.setId(1L);
        product.setProductCode("P001");
        product.setProductName("东北大米");
        product.setStatus(1);
        product.setCategoryId(2L);
        product.setBaseUnitId(3L);
        when(categoryMapper.selectById(2L)).thenReturn(category(2L, 0L, 1));
        BaseUnit unit = new BaseUnit(); unit.setId(3L); unit.setStatus(1);
        when(unitMapper.selectById(3L)).thenReturn(unit);
        when(baseProductMapper.selectList(any())).thenReturn(List.of(product));

        List<BasicOptionResponse> options = service.productOptions(null);

        assertEquals(1, options.size());
        assertEquals(1L, options.get(0).getId());
        assertEquals("P001", options.get(0).getCode());
        assertEquals("东北大米", options.get(0).getName());
        assertEquals(1, options.get(0).getStatus());
    }

    @Test
    void validatePurchaseMasterDataRejectsDisabledSupplier() {
        BasicDataService service = service();
        BaseWarehouse warehouse = new BaseWarehouse();
        warehouse.setStatus(1);
        BaseSupplier supplier = new BaseSupplier();
        supplier.setStatus(0);
        when(warehouseMapper.selectById(1L)).thenReturn(warehouse);
        when(supplierMapper.selectById(2L)).thenReturn(supplier);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.validatePurchaseMasterData(1L, 2L, List.of(3L), List.of(4L)));

        assertEquals(400, exception.getCode());
        assertEquals("basic-data-disabled", exception.getMessage());
    }

    @Test
    void validateSaleMasterDataRejectsDisabledCustomer() {
        BasicDataService service = service();
        BaseWarehouse warehouse = new BaseWarehouse();
        warehouse.setStatus(1);
        BaseCustomer customer = new BaseCustomer();
        customer.setStatus(0);
        when(warehouseMapper.selectById(1L)).thenReturn(warehouse);
        when(customerMapper.selectById(2L)).thenReturn(customer);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.validateSaleMasterData(1L, 2L, List.of(3L), List.of(4L)));

        assertEquals(400, exception.getCode());
        assertEquals("basic-data-disabled", exception.getMessage());
    }

    @Test
    void warehouseValidationChecksRequestedRecordRatherThanTableCount() {
        BaseWarehouse warehouse = new BaseWarehouse();
        warehouse.setId(7L);
        warehouse.setStatus(0);
        org.mockito.Mockito.lenient().when(warehouseMapper.selectById(7L)).thenReturn(warehouse);
        org.mockito.Mockito.lenient().when(warehouseMapper.selectCount(any())).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service().assertWarehouseEnabled(7L));
    }

    @Test
    void duplicateUnitCodeIsRejectedBeforeInsert() {
        UnitSaveRequest request = new UnitSaveRequest();
        request.setUnitCode("KG");
        request.setUnitName("公斤");
        when(unitMapper.selectCount(any())).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service().createUnit(request));
        verify(unitMapper, never()).insert(any(BaseUnit.class));
    }

    @Test
    void categoryCannotBeMovedBelowItsDescendant() {
        BaseProductCategory parent = category(1L, 0L, 1);
        BaseProductCategory child = category(2L, 1L, 1);
        when(categoryMapper.selectById(1L)).thenReturn(parent);
        when(categoryMapper.selectById(2L)).thenReturn(child);
        CategorySaveRequest request = new CategorySaveRequest();
        request.setCategoryName("大米");
        request.setParentId(2L);
        assertThrows(BusinessException.class, () -> service().updateCategory(1L, request));
    }

    @Test
    void categoryWithProductsCannotBeDeleted() {
        when(categoryMapper.selectById(1L)).thenReturn(category(1L, 0L, 1));
        when(baseProductMapper.selectCount(any())).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service().deleteCategory(1L));
        verify(categoryMapper, never()).deleteById(1L);
    }

    @Test
    void unitUsedByProductCannotBeDeleted() {
        BaseUnit unit = new BaseUnit();
        unit.setId(1L);
        when(unitMapper.selectById(1L)).thenReturn(unit);
        when(baseProductMapper.selectCount(any())).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service().deleteUnit(1L));
    }

    @Test
    void supplierWithOrdersCannotBeDeleted() {
        when(supplierMapper.selectById(1L)).thenReturn(new BaseSupplier());
        when(purchaseInMapper.selectCount(any())).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service().deleteSupplier(1L));
    }

    @Test
    void conversionCannotUseSameUnits() {
        UnitConvertSaveRequest request = new UnitConvertSaveRequest();
        request.setProductId(1L);
        request.setFromUnitId(2L);
        request.setToUnitId(2L);
        request.setConvertRate(BigDecimal.TEN);
        assertThrows(BusinessException.class, () -> service().createUnitConvert(request));
    }

    @Test
    void invalidCreateStatusIsRejected() {
        UnitSaveRequest request = new UnitSaveRequest();
        request.setUnitCode("KG");
        request.setUnitName("公斤");
        request.setStatus(7);
        assertThrows(BusinessException.class, () -> service().createUnit(request));
    }

    @Test
    void historicalProductDetailIncludesDisabledCategoryAndUnitNames() {
        BaseProduct product = new BaseProduct();
        product.setId(1L);
        product.setCategoryId(2L);
        product.setBaseUnitId(3L);
        BaseProductCategory category = category(2L, 0L, 0);
        category.setCategoryName("大米");
        BaseUnit unit = new BaseUnit();
        unit.setUnitName("斤");
        unit.setStatus(0);
        when(baseProductMapper.selectById(1L)).thenReturn(product);
        when(categoryMapper.selectById(2L)).thenReturn(category);
        when(unitMapper.selectById(3L)).thenReturn(unit);
        assertEquals("大米", service().productDetail(1L).getCategoryName());
        assertEquals("斤", service().productDetail(1L).getBaseUnitName());
    }

    @Test
    void multiHopConversionMultipliesSelectedUnitIntoBaseUnit() {
        enabledProduct();
        when(unitConvertMapper.selectList(any())).thenReturn(List.of(edge(2L, 3L, "10"), edge(3L, 4L, "10")));
        assertEquals(0, new BigDecimal("100").compareTo(service().productUnitRate(1L, 2L)));
    }

    @Test
    void conversionLookupCanReverseAnEdge() {
        enabledProduct();
        when(unitConvertMapper.selectList(any())).thenReturn(List.of(edge(4L, 2L, "0.01")));
        assertEquals(0, new BigDecimal("100").compareTo(service().productUnitRate(1L, 2L)));
    }

    @Test
    void conversionRejectsUnitUnreachableFromBaseUnit() {
        enabledProduct();
        when(unitConvertMapper.selectList(any())).thenReturn(List.of());
        BusinessException exception = assertThrows(BusinessException.class, () -> service().productUnitRate(1L, 2L));
        assertEquals("product-unit-unreachable", exception.getMessage());
    }

    @Test
    void conversionRejectsInconsistentPaths() {
        enabledProduct();
        when(unitConvertMapper.selectList(any())).thenReturn(List.of(edge(2L, 3L, "10"), edge(3L, 4L, "10"), edge(2L, 4L, "50")));
        BusinessException exception = assertThrows(BusinessException.class, () -> service().productUnitRate(1L, 2L));
        assertEquals("convert-rate-inconsistent", exception.getMessage());
    }

    @Test
    void conversionRejectsDirectedCycle() {
        enabledProduct();
        when(unitConvertMapper.selectList(any())).thenReturn(List.of(edge(2L, 3L, "10"), edge(3L, 4L, "10")));
        UnitConvertSaveRequest request = conversion(4L, 2L, "0.01");
        BusinessException exception = assertThrows(BusinessException.class, () -> service().createUnitConvert(request));
        assertEquals("convert-cycle", exception.getMessage());
    }

    @Test
    void unitOptionsExcludePathsThroughDisabledUnit() {
        enabledProduct();
        BaseUnit disabled = new BaseUnit(); disabled.setId(3L); disabled.setStatus(0);
        when(unitMapper.selectById(3L)).thenReturn(disabled);
        when(unitConvertMapper.selectList(any())).thenReturn(List.of(edge(2L, 3L, "10"), edge(3L, 4L, "10")));
        assertEquals(List.of(4L), service().productUnitOptions(1L).stream().map(BasicOptionResponse::getId).toList());
    }

    @Test
    void categoryOptionsExcludeEnabledDescendantOfDisabledParent() {
        BaseProductCategory parent = category(1L, 0L, 0); parent.setCategoryName("停用父分类");
        BaseProductCategory child = category(2L, 1L, 1); child.setCategoryName("子分类");
        when(categoryMapper.selectList(any())).thenReturn(List.of(parent, child));
        assertEquals(List.of(), service().categoryOptions(null));
    }

    @Test
    void warehouseWithStockCannotBeDeleted() {
        when(warehouseMapper.selectById(1L)).thenReturn(new BaseWarehouse());
        when(invStockMapper.selectCount(any())).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service().deleteWarehouse(1L));
        verify(warehouseMapper, never()).deleteById(1L);
    }

    @Test
    void customerWithStatisticalSummaryCannotBeDeleted() {
        when(customerMapper.selectById(1L)).thenReturn(new BaseCustomer());
        when(customerMapper.countSummaryReferences(1L)).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service().deleteCustomer(1L));
    }

    @Test
    void databaseDuplicateKeyIsTranslatedToBusinessFailure() {
        UnitSaveRequest request = new UnitSaveRequest(); request.setUnitCode("KG"); request.setUnitName("公斤");
        when(unitMapper.insert(any(BaseUnit.class))).thenThrow(new org.springframework.dao.DuplicateKeyException("race"));
        BusinessException exception = assertThrows(BusinessException.class, () -> service().createUnit(request));
        assertEquals("basic-data-duplicate", exception.getMessage());
    }

    @Test
    void productExportIncludesEveryMatchingRecordAndReadableHistoricalNames() {
        BaseProduct first = new BaseProduct(); first.setId(1L); first.setProductName("大米"); first.setCategoryId(5L); first.setBaseUnitId(4L);
        BaseProduct second = new BaseProduct(); second.setId(2L); second.setProductName("小米"); second.setCategoryId(5L); second.setBaseUnitId(4L);
        BaseProductCategory category = category(5L, 0L, 0); category.setCategoryName("粮食");
        BaseUnit unit = new BaseUnit(); unit.setUnitName("斤"); unit.setStatus(0);
        when(baseProductMapper.selectList(any())).thenReturn(List.of(first, second));
        when(categoryMapper.selectById(5L)).thenReturn(category);
        when(unitMapper.selectById(4L)).thenReturn(unit);
        ProductQueryRequest request = new ProductQueryRequest(); request.setPageSize(1); request.setPageNum(8);
        List<java.util.Map<String, Object>> rows = service().exportProducts(request);
        assertEquals(2, rows.size());
        assertEquals("粮食", rows.get(0).get("分类"));
        assertEquals("斤", rows.get(1).get("基础单位"));
        verify(baseProductMapper, never()).selectPage(any(), any());
    }

    @Test
    void editingWarehouseWithoutStatusPreservesDisabledState() {
        BaseWarehouse warehouse = new BaseWarehouse(); warehouse.setId(1L); warehouse.setStatus(0);
        when(warehouseMapper.selectById(1L)).thenReturn(warehouse);
        WarehouseSaveRequest request = new WarehouseSaveRequest(); request.setWarehouseCode("WH1"); request.setWarehouseName("旧仓库");
        assertEquals(0, service().updateWarehouse(1L, request).getStatus());
    }

    @Test
    void productBaseUnitCannotChangeAfterBusinessReference() {
        enabledProduct();
        when(purchaseInItemMapper.selectCount(any())).thenReturn(1L);
        ProductSaveRequest request = new ProductSaveRequest(); request.setProductCode("P1"); request.setProductName("大米");
        request.setCategoryId(5L); request.setBaseUnitId(2L);
        BusinessException exception = assertThrows(BusinessException.class, () -> service().updateProduct(1L, request));
        assertEquals("basic-data-in-use", exception.getMessage());
        verify(baseProductMapper, never()).updateById(any(BaseProduct.class));
    }

    private void enabledProduct() {
        BaseProduct product = new BaseProduct(); product.setId(1L); product.setCategoryId(5L); product.setBaseUnitId(4L); product.setStatus(1);
        org.mockito.Mockito.lenient().when(baseProductMapper.selectById(1L)).thenReturn(product);
        org.mockito.Mockito.lenient().when(categoryMapper.selectById(5L)).thenReturn(category(5L, 0L, 1));
        for (Long id : List.of(2L, 3L, 4L)) {
            BaseUnit unit = new BaseUnit(); unit.setId(id); unit.setStatus(1); unit.setUnitName("单位" + id);
            org.mockito.Mockito.lenient().when(unitMapper.selectById(id)).thenReturn(unit);
        }
    }

    private BaseProductUnitConvert edge(Long from, Long to, String rate) {
        BaseProductUnitConvert edge = new BaseProductUnitConvert(); edge.setProductId(1L);
        edge.setFromUnitId(from); edge.setToUnitId(to); edge.setConvertRate(new BigDecimal(rate)); edge.setStatus(1);
        return edge;
    }

    private UnitConvertSaveRequest conversion(Long from, Long to, String rate) {
        UnitConvertSaveRequest request = new UnitConvertSaveRequest(); request.setProductId(1L);
        request.setFromUnitId(from); request.setToUnitId(to); request.setConvertRate(new BigDecimal(rate));
        return request;
    }

    private BaseProductCategory category(Long id, Long parentId, int status) {
        BaseProductCategory category = new BaseProductCategory();
        category.setId(id);
        category.setParentId(parentId);
        category.setStatus(status);
        return category;
    }

    private BasicDataService service() {
        return new BasicDataService(baseProductMapper, categoryMapper, unitMapper, unitConvertMapper,
                warehouseMapper, supplierMapper, customerMapper, purchaseInItemMapper, saleOutItemMapper,
                invStockMapper, invBatchMapper, invStockFlowMapper, purchaseInMapper, saleOutMapper, sysUserMapper);
    }
}
