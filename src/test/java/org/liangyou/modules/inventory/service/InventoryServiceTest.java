package org.liangyou.modules.inventory.service;

import org.junit.jupiter.api.Test;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.inventory.entity.*;
import org.liangyou.modules.inventory.mapper.*;
import org.liangyou.modules.inventory.dto.InventoryUpdateRequest;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.basic.service.ExcelExportService;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InventoryServiceTest {
    @Test
    void nonzeroInventoryCannotBeDeleted() {
        var stocks = mock(InvStockMapper.class);
        var stock = stock();
        when(stocks.selectById(1L)).thenReturn(stock);
        var ledger = mock(InventoryLedger.class);
        when(ledger.requireStock(1L, 3L)).thenReturn(stock());
        var service = new InventoryService(stocks, mock(InvBatchMapper.class), mock(InvStockFlowMapper.class),
                mock(BasicDataService.class), mock(WarehouseMutationLock.class), ledger, new ExcelExportService());
        assertThrows(BusinessException.class, () -> service.delete(1L));
        verify(stocks, never()).deleteById(1L);
    }

    @Test
    void rejectsAdjustmentThatChangesLockedQuantityImplicitly() {
        var stocks = mock(InvStockMapper.class);
        when(stocks.selectById(1L)).thenReturn(stock());
        var ledger = mock(InventoryLedger.class);
        when(ledger.requireStock(1L, 3L)).thenReturn(stock());
        var service = new InventoryService(stocks, mock(InvBatchMapper.class), mock(InvStockFlowMapper.class),
                mock(BasicDataService.class), mock(WarehouseMutationLock.class), ledger, new ExcelExportService());
        var request = new InventoryUpdateRequest();
        request.setTotalQty(new BigDecimal("15")); request.setAvailableQty(new BigDecimal("14")); request.setRemark("count");
        assertThrows(BusinessException.class, () -> service.update(1L, request));
        verify(stocks, never()).updateById(any(InvStock.class));
    }

    private InvStock stock() {
        var stock = new InvStock(); stock.setId(1L); stock.setWarehouseId(1L); stock.setProductId(3L);
        stock.setTotalQty(BigDecimal.TEN); stock.setAvailableQty(BigDecimal.TEN); stock.setLockedQty(BigDecimal.ZERO);
        return stock;
    }
}
