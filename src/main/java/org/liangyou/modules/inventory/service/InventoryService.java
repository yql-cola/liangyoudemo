package org.liangyou.modules.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.PageResponse;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.basic.entity.BaseProductCategory;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.basic.service.ExcelExportService;
import org.liangyou.modules.inventory.dto.*;
import org.liangyou.modules.inventory.entity.*;
import org.liangyou.modules.inventory.mapper.*;
import org.liangyou.modules.inventory.vo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InvStockMapper invStockMapper;
    private final InvBatchMapper invBatchMapper;
    private final InvStockFlowMapper invStockFlowMapper;
    private final BasicDataService basicDataService;
    private final WarehouseMutationLock locks;
    private final InventoryLedger ledger;
    private final ExcelExportService excel;

    private LambdaQueryWrapper<InvStock> stockQuery(InventoryQueryRequest request) {
        return new LambdaQueryWrapper<InvStock>()
                .eq(request.getWarehouseId() != null, InvStock::getWarehouseId, request.getWarehouseId())
                .eq(request.getProductId() != null, InvStock::getProductId, request.getProductId())
                .inSql(request.getCategoryId() != null, InvStock::getProductId, categoryProducts(request.getCategoryId()))
                .orderByDesc(InvStock::getId);
    }

    public PageResponse<InventoryResponse> query(InventoryQueryRequest request) {
        Page<InvStock> result = invStockMapper.selectPage(new Page<>(Math.max(1, request.getPageNum()),
                Math.max(1, Math.min(100, request.getPageSize()))), stockQuery(request));
        return new PageResponse<>(result.getRecords().stream().map(this::toInventoryResponse).toList(), result.getTotal());
    }

    public List<InventoryBatchResponse> batches(Long warehouseId, Long productId) {
        return batches(warehouseId, productId, null);
    }

    public List<InventoryBatchResponse> batches(Long warehouseId, Long productId, Long categoryId) {
        return invBatchMapper.selectList(new LambdaQueryWrapper<InvBatch>()
                        .eq(warehouseId != null, InvBatch::getWarehouseId, warehouseId)
                        .eq(productId != null, InvBatch::getProductId, productId)
                        .inSql(categoryId != null, InvBatch::getProductId, categoryProducts(categoryId))
                        .orderByAsc(InvBatch::getInboundTime).orderByAsc(InvBatch::getId))
                .stream().map(this::toBatchResponse).toList();
    }

    public List<InventoryFlowResponse> flows(InventoryFlowQueryRequest request) {
        if (request.getBizTimeFrom() != null && request.getBizTimeTo() != null
                && request.getBizTimeFrom().isAfter(request.getBizTimeTo()))
            throw new BusinessException(400, "invalid-date-range");
        return invStockFlowMapper.selectList(new LambdaQueryWrapper<InvStockFlow>()
                        .eq(request.getWarehouseId() != null, InvStockFlow::getWarehouseId, request.getWarehouseId())
                        .eq(request.getProductId() != null, InvStockFlow::getProductId, request.getProductId())
                        .inSql(request.getCategoryId() != null, InvStockFlow::getProductId, categoryProducts(request.getCategoryId()))
                        .ge(request.getBizTimeFrom() != null, InvStockFlow::getBizTime, request.getBizTimeFrom())
                        .le(request.getBizTimeTo() != null, InvStockFlow::getBizTime, request.getBizTimeTo())
                        .orderByDesc(InvStockFlow::getBizTime).orderByDesc(InvStockFlow::getId))
                .stream().map(this::toFlowResponse).toList();
    }

    public byte[] exportStocks(InventoryQueryRequest request) {
        return excel.export("inventories", rows(invStockMapper.selectList(stockQuery(request)).stream()
                .map(this::toInventoryResponse).toList()));
    }

    public byte[] exportBatches(InventoryQueryRequest request) {
        return excel.export("inventory-batches", rows(batches(request.getWarehouseId(), request.getProductId(), request.getCategoryId())));
    }

    public byte[] exportFlows(InventoryFlowQueryRequest request) {
        return excel.export("inventory-flows", rows(flows(request)));
    }

    @Transactional
    public void update(Long id, InventoryUpdateRequest request) {
        if (request == null || !StringUtils.hasText(request.getRemark()))
            throw new BusinessException(400, "adjustment-reason-required");
        BigDecimal total = BusinessNumbers.quantity(request.getTotalQty(), false);
        BigDecimal available = BusinessNumbers.quantity(request.getAvailableQty(), false);
        InvStock initial = requireById(id);
        locks.lock(initial.getWarehouseId());
        InvStock stock = ledger.requireStock(initial.getWarehouseId(), initial.getProductId());
        basicDataService.assertWarehouseEnabled(stock.getWarehouseId());
        basicDataService.assertProductEnabled(stock.getProductId());
        if (stock.getLockedQty().signum() < 0 || total.compareTo(available.add(stock.getLockedQty())) != 0)
            throw new BusinessException(400, "inventory-quantity-mismatch");
        List<InvBatch> batches = ledger.fifo(stock.getWarehouseId(), stock.getProductId());
        BigDecimal batchTotal = batches.stream().map(InvBatch::getRemainQty).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (batchTotal.compareTo(stock.getTotalQty()) != 0) InventoryLedger.conflict();
        BigDecimal change = total.subtract(stock.getTotalQty());
        if (change.signum() > 0) {
            // Count gains use the weighted cost of remaining base units; an empty stock starts at zero cost.
            BigDecimal value = batches.stream().map(batch -> batch.getRemainQty().multiply(batch.getUnitPrice()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal cost = batchTotal.signum() == 0 ? BigDecimal.ZERO : value.divide(batchTotal, 4, RoundingMode.HALF_UP);
            InvBatch batch = new InvBatch();
            batch.setWarehouseId(stock.getWarehouseId()); batch.setProductId(stock.getProductId());
            batch.setSourceType("manual_adjust"); batch.setSourceId(stock.getId());
            batch.setBatchNo("ADJ" + UUID.randomUUID().toString().replace("-", ""));
            batch.setInboundTime(LocalDateTime.now()); batch.setUnitPrice(cost);
            batch.setInitQty(change); batch.setRemainQty(change); batch.setStatus(1);
            if (invBatchMapper.insert(batch) != 1) InventoryLedger.conflict();
            ledger.change(stock, change);
            ledger.flow(stock, batch.getId(), "inventory_adjust", stock.getId(), null, change,
                    LocalDateTime.now(), request.getRemark());
        } else if (change.signum() < 0) {
            BigDecimal remaining = change.negate();
            for (InvBatch batch : batches) {
                if (remaining.signum() == 0) break;
                BigDecimal deducted = remaining.min(batch.getRemainQty());
                batch.setRemainQty(batch.getRemainQty().subtract(deducted)); ledger.saveBatch(batch);
                ledger.change(stock, deducted.negate()); remaining = remaining.subtract(deducted);
                ledger.flow(stock, batch.getId(), "inventory_adjust", stock.getId(), null, deducted.negate(),
                        LocalDateTime.now(), request.getRemark());
            }
            if (remaining.signum() != 0) InventoryLedger.conflict();
        }
    }

    @Transactional
    public void delete(Long id) {
        InvStock initial = requireById(id);
        locks.lock(initial.getWarehouseId());
        InvStock stock = ledger.requireStock(initial.getWarehouseId(), initial.getProductId());
        if (stock.getTotalQty().signum() != 0 || stock.getAvailableQty().signum() != 0 || stock.getLockedQty().signum() != 0
                || invStockMapper.deleteUnusedZero(id) != 1)
            throw new BusinessException(400, "inventory-in-use");
    }

    private InvStock requireById(Long id) {
        InvStock stock = invStockMapper.selectById(id);
        if (stock == null) throw new BusinessException(404, "inventory-not-found");
        return stock;
    }

    private String categoryProducts(Long categoryId) {
        if (categoryId == null) return "SELECT id FROM base_product WHERE 1=0";
        Set<Long> scope = new LinkedHashSet<>(); scope.add(categoryId);
        Set<Long> visited = new HashSet<>();
        Deque<BaseProductCategory> pending = new ArrayDeque<>(basicDataService.categoryTree());
        while (!pending.isEmpty()) {
            BaseProductCategory category = pending.removeFirst();
            if (!visited.add(category.getId())) continue;
            if (scope.contains(category.getParentId())) scope.add(category.getId());
            pending.addAll(category.getChildren());
        }
        // IDs originate from typed request/database values; disabled categories remain visible in history.
        String ids = scope.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        return "SELECT id FROM base_product WHERE category_id IN (" + ids + ")";
    }

    private void enrich(Object response, Long warehouseId, Long productId) {
        var product = basicDataService.productDetail(productId);
        var bean = new BeanWrapperImpl(response);
        bean.setPropertyValue("warehouseName", basicDataService.warehouseDetail(warehouseId).getWarehouseName());
        bean.setPropertyValue("productName", product.getProductName()); bean.setPropertyValue("spec", product.getSpec());
        bean.setPropertyValue("categoryId", product.getCategoryId()); bean.setPropertyValue("categoryName", product.getCategoryName());
        bean.setPropertyValue("unitId", product.getBaseUnitId()); bean.setPropertyValue("unitName", product.getBaseUnitName());
    }

    private InventoryResponse toInventoryResponse(InvStock stock) {
        var response = new InventoryResponse(); BeanUtils.copyProperties(stock, response);
        enrich(response, stock.getWarehouseId(), stock.getProductId()); return response;
    }

    private InventoryBatchResponse toBatchResponse(InvBatch batch) {
        var response = new InventoryBatchResponse(); BeanUtils.copyProperties(batch, response);
        enrich(response, batch.getWarehouseId(), batch.getProductId()); return response;
    }

    private InventoryFlowResponse toFlowResponse(InvStockFlow flow) {
        var response = new InventoryFlowResponse(); BeanUtils.copyProperties(flow, response);
        enrich(response, flow.getWarehouseId(), flow.getProductId()); return response;
    }

    private List<Map<String, Object>> rows(List<?> records) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object record : records) {
            var bean = new BeanWrapperImpl(record);
            Map<String, Object> row = new LinkedHashMap<>();
            for (var property : bean.getPropertyDescriptors()) {
                if (!property.getName().equals("class")) row.put(property.getName(), bean.getPropertyValue(property.getName()));
            }
            result.add(row);
        }
        return result;
    }
}
