package org.liangyou.modules.purchase.service;

import org.junit.jupiter.api.Test;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.inventory.entity.InvStock;
import org.liangyou.modules.inventory.mapper.*;
import org.liangyou.modules.inventory.entity.InvBatch;
import org.liangyou.modules.inventory.entity.InvStockFlow;
import org.liangyou.modules.inventory.service.*;
import static org.mockito.ArgumentMatchers.any;
import org.liangyou.modules.purchase.dto.*;
import org.liangyou.modules.purchase.entity.*;
import org.liangyou.modules.purchase.mapper.*;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PurchaseInServiceTest {
    @Test
    void purchaseConvertsSelectedUnitsAndComputesCanonicalAmount() {
        var headers = mock(BizPurchaseInMapper.class);
        var items = mock(BizPurchaseInItemMapper.class);
        var stocks = mock(InvStockMapper.class);
        var batches = mock(InvBatchMapper.class);
        var flows = mock(InvStockFlowMapper.class);
        var basic = mock(BasicDataService.class);
        when(basic.productUnitRate(3L, 4L)).thenReturn(new BigDecimal("10"));
        when(headers.insert(any(BizPurchaseIn.class))).thenReturn(1);
        when(items.insert(any(BizPurchaseInItem.class))).thenReturn(1);
        when(stocks.insert(any(InvStock.class))).thenReturn(1);
        when(batches.insert(any(InvBatch.class))).thenReturn(1);
        when(flows.insert(any(InvStockFlow.class))).thenReturn(1);
        var service = new PurchaseInService(headers, items, batches, mock(InvSaleBatchDeductionMapper.class), basic,
                mock(WarehouseMutationLock.class), new InventoryLedger(stocks, batches, flows));
        var item = new PurchaseInItemRequest();
        item.setProductId(3L); item.setUnitId(4L); item.setQty(new BigDecimal("2"));
        item.setUnitPrice(new BigDecimal("3")); item.setAmount(new BigDecimal("999"));
        item.setBatchNo("B1"); item.setInboundTime(LocalDateTime.now());
        var request = new PurchaseInCreateRequest();
        request.setWarehouseId(1L); request.setSupplierId(2L); request.setBizDate(LocalDate.now()); request.setItems(List.of(item));
        service.create(request);
        var stock = ArgumentCaptor.forClass(InvStock.class);
        verify(stocks).insert(stock.capture());
        assertEquals(0, new BigDecimal("20").compareTo(stock.getValue().getAvailableQty()));
        var header = ArgumentCaptor.forClass(BizPurchaseIn.class);
        verify(headers).insert(header.capture());
        assertEquals(0, new BigDecimal("6").compareTo(header.getValue().getTotalAmount()));
    }
}
