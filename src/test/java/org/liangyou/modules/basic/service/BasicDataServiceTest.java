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

    private BasicDataService service() {
        return new BasicDataService(baseProductMapper, categoryMapper, unitMapper, unitConvertMapper,
                warehouseMapper, supplierMapper, customerMapper, purchaseInItemMapper, saleOutItemMapper,
                invStockMapper, invBatchMapper, invStockFlowMapper, purchaseInMapper, saleOutMapper, sysUserMapper);
    }
}
