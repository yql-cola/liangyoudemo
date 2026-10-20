package org.liangyou.modules.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.inventory.entity.*;
import org.liangyou.modules.inventory.mapper.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Callers hold WarehouseMutationLock for every affected warehouse. Quantities use base units. */
@Service
@RequiredArgsConstructor
public class InventoryLedger {
    private final InvStockMapper stocks;
    private final InvBatchMapper batches;
    private final InvStockFlowMapper flows;

    public InvStock stock(Long warehouseId, Long productId) {
        return stocks.selectOne(new LambdaQueryWrapper<InvStock>().eq(InvStock::getWarehouseId, warehouseId)
                .eq(InvStock::getProductId, productId).last("FOR UPDATE"));
    }

    public InvStock requireStock(Long warehouseId, Long productId) {
        InvStock stock = stock(warehouseId, productId);
        if (stock == null) conflict();
        return stock;
    }

    public InvStock inbound(Long warehouseId, Long productId, BigDecimal qty) {
        InvStock stock = stock(warehouseId, productId);
        if (stock == null) {
            stock = new InvStock(); stock.setWarehouseId(warehouseId); stock.setProductId(productId);
            stock.setLockedQty(BigDecimal.ZERO); stock.setTotalQty(qty); stock.setAvailableQty(qty);
            if (stocks.insert(stock) != 1) conflict();
        } else change(stock, qty);
        return stock;
    }

    public void change(InvStock stock, BigDecimal delta) {
        BigDecimal total = stock.getTotalQty().add(delta);
        BigDecimal available = stock.getAvailableQty().add(delta);
        if (total.signum() < 0 || available.signum() < 0 || stock.getLockedQty().signum() < 0
                || total.compareTo(available.add(stock.getLockedQty())) != 0) conflict();
        BusinessNumbers.quantity(total, false); BusinessNumbers.quantity(available, false);
        stock.setTotalQty(total); stock.setAvailableQty(available);
        if (stocks.updateById(stock) != 1) conflict();
    }

    public void reserve(InvStock stock, BigDecimal qty) {
        if (qty.signum() <= 0 || stock.getAvailableQty().compareTo(qty) < 0) {
            throw new BusinessException(400, "insufficient-stock");
        }
        stock.setAvailableQty(stock.getAvailableQty().subtract(qty));
        stock.setLockedQty(stock.getLockedQty().add(qty));
        saveReservedStock(stock);
    }

    public void release(InvStock stock, BigDecimal qty) {
        if (qty.signum() <= 0 || stock.getLockedQty().compareTo(qty) < 0) conflict();
        stock.setLockedQty(stock.getLockedQty().subtract(qty));
        stock.setAvailableQty(stock.getAvailableQty().add(qty));
        saveReservedStock(stock);
    }

    public void consume(InvStock stock, BigDecimal qty) {
        if (qty.signum() <= 0 || stock.getLockedQty().compareTo(qty) < 0 || stock.getTotalQty().compareTo(qty) < 0) conflict();
        stock.setLockedQty(stock.getLockedQty().subtract(qty));
        stock.setTotalQty(stock.getTotalQty().subtract(qty));
        saveReservedStock(stock);
    }

    private void saveReservedStock(InvStock stock) {
        if (stock.getTotalQty().compareTo(stock.getAvailableQty().add(stock.getLockedQty())) != 0) conflict();
        if (stocks.updateById(stock) != 1) conflict();
    }

    public List<InvBatch> fifo(Long warehouseId, Long productId) {
        return batches.selectList(new LambdaQueryWrapper<InvBatch>().eq(InvBatch::getWarehouseId, warehouseId)
                .eq(InvBatch::getProductId, productId).gt(InvBatch::getRemainQty, BigDecimal.ZERO)
                .orderByAsc(InvBatch::getInboundTime).orderByAsc(InvBatch::getId).last("FOR UPDATE"));
    }

    public void saveBatch(InvBatch batch) {
        if (batch.getRemainQty().signum() < 0 || batch.getRemainQty().compareTo(batch.getInitQty()) > 0) conflict();
        if (batches.updateById(batch) != 1) conflict();
    }

    public void flow(InvStock stock, Long batchId, String type, Long bizId, Long itemId,
                     BigDecimal delta, LocalDateTime time, String remark) {
        InvStockFlow flow = new InvStockFlow();
        flow.setWarehouseId(stock.getWarehouseId()); flow.setProductId(stock.getProductId()); flow.setBatchId(batchId);
        // Validate all new ledger entries against stable business codes before persisting.
        InventoryMovementType movement = InventoryMovementType.fromBusinessCode(type);
        flow.setFlowType(movement.flowCode()); flow.setBizType(movement.businessCode()); flow.setBizId(bizId); flow.setBizItemId(itemId);
        flow.setChangeQty(delta); flow.setAfterQty(stock.getAvailableQty()); flow.setBizTime(time); flow.setRemark(remark);
        if (flows.insert(flow) != 1) conflict();
    }

    public static void conflict() { throw new BusinessException(400, "inventory-rollback-conflict"); }
}
