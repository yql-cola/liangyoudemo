package org.liangyou.modules.sale.service;

import org.junit.jupiter.api.Test;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.inventory.entity.InvStock;
import org.liangyou.modules.inventory.mapper.*;
import org.liangyou.modules.inventory.service.*;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.sale.dto.*;
import org.liangyou.modules.sale.mapper.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SaleOutServiceTest {
    @Test
    void negativeSaleCannotIncreaseStock() {
        var headers = mock(BizSaleOutMapper.class);
        var stocks = mock(InvStockMapper.class);
        var stock = new InvStock(); stock.setTotalQty(BigDecimal.TEN); stock.setAvailableQty(BigDecimal.TEN);
        when(stocks.selectOne(any())).thenReturn(stock);
        var service = new SaleOutService(headers, mock(BizSaleOutItemMapper.class), mock(InvBatchMapper.class),
                mock(InvSaleBatchDeductionMapper.class), mock(BasicDataService.class), mock(WarehouseMutationLock.class), mock(InventoryLedger.class), null);
        var item = new SaleOutItemRequest();
        item.setProductId(3L); item.setUnitId(4L); item.setQty(BigDecimal.ONE.negate());
        item.setUnitPrice(BigDecimal.ONE); item.setAmount(BigDecimal.ONE);
        var request = new SaleOutCreateRequest();
        request.setWarehouseId(1L); request.setCustomerId(2L); request.setBizDate(LocalDate.now()); request.setItems(List.of(item));
        assertThrows(BusinessException.class, () -> service.create(request));
        verify(stocks, never()).updateById(any(InvStock.class));
    }
}
