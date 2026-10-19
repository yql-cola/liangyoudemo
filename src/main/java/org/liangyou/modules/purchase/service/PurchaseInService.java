package org.liangyou.modules.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.PageResponse;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.inventory.entity.*;
import org.liangyou.modules.inventory.mapper.*;
import org.liangyou.modules.inventory.service.*;
import org.liangyou.modules.purchase.dto.*;
import org.liangyou.modules.purchase.entity.*;
import org.liangyou.modules.purchase.mapper.*;
import org.liangyou.modules.purchase.vo.PurchaseInDetailResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PurchaseInService {
    private final BizPurchaseInMapper purchaseInMapper;
    private final BizPurchaseInItemMapper purchaseInItemMapper;
    private final InvBatchMapper invBatchMapper;
    private final InvSaleBatchDeductionMapper invSaleBatchDeductionMapper;
    private final BasicDataService basicDataService;
    private final WarehouseMutationLock locks;
    private final InventoryLedger ledger;

    @Transactional
    public Long create(PurchaseInCreateRequest request) {
        requireRequest(request);
        locks.lock(request.getWarehouseId());
        validateMasterData(request);
        BizPurchaseIn header = new BizPurchaseIn();
        header.setOrderNo("PI" + UUID.randomUUID().toString().replace("-", ""));
        applyHeader(header, request);
        if (purchaseInMapper.insert(header) != 1) InventoryLedger.conflict();
        createItems(header, request);
        return header.getId();
    }

    @Transactional
    public void update(Long id, PurchaseInCreateRequest request) {
        requireRequest(request);
        BizPurchaseIn header = requireHeader(id);
        locks.lock(header.getWarehouseId(), request.getWarehouseId());
        validateMasterData(request);
        rollbackItems(id);
        applyHeader(header, request);
        if (purchaseInMapper.updateById(header) != 1) InventoryLedger.conflict();
        createItems(header, request);
    }

    @Transactional
    public void delete(Long id) {
        BizPurchaseIn header = requireHeader(id);
        locks.lock(header.getWarehouseId());
        rollbackItems(id);
        if (purchaseInMapper.deleteById(id) != 1) InventoryLedger.conflict();
    }

    private BizPurchaseIn requireHeader(Long id) {
        BizPurchaseIn header = purchaseInMapper.selectOne(new LambdaQueryWrapper<BizPurchaseIn>()
                .eq(BizPurchaseIn::getId, id).last("FOR UPDATE"));
        if (header == null) throw new BusinessException(404, "purchase-in-not-found");
        return header;
    }

    private void requireRequest(PurchaseInCreateRequest request) {
        if (request == null || request.getWarehouseId() == null || request.getSupplierId() == null
                || request.getBizDate() == null || request.getItems() == null || request.getItems().isEmpty())
            throw new BusinessException(400, "invalid-business-request");
        for (PurchaseInItemRequest item : request.getItems()) {
            if (item == null || item.getProductId() == null || item.getUnitId() == null)
                throw new BusinessException(400, "invalid-business-request");
            BusinessNumbers.quantity(item.getQty(), true);
            BusinessNumbers.quantity(item.getUnitPrice(), false);
            if (item.getAmount() != null && item.getAmount().signum() < 0)
                throw new BusinessException(400, "invalid-business-number");
            if (!StringUtils.hasText(item.getBatchNo()) || item.getInboundTime() == null)
                throw new BusinessException(400, "invalid-business-request");
            if (item.getProductionDate() != null && item.getExpiryDate() != null
                    && item.getExpiryDate().isBefore(item.getProductionDate()))
                throw new BusinessException(400, "invalid-expiry-date");
        }
    }

    private void validateMasterData(PurchaseInCreateRequest request) {
        basicDataService.validatePurchaseMasterData(request.getWarehouseId(), request.getSupplierId(),
                request.getItems().stream().map(PurchaseInItemRequest::getProductId).toList(),
                request.getItems().stream().map(PurchaseInItemRequest::getUnitId).toList());
    }

    private void applyHeader(BizPurchaseIn header, PurchaseInCreateRequest request) {
        header.setWarehouseId(request.getWarehouseId()); header.setSupplierId(request.getSupplierId());
        header.setBizDate(request.getBizDate()); header.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        header.setRemark(request.getRemark());
        header.setTotalAmount(BusinessNumbers.money(request.getItems().stream()
                .map(item -> BusinessNumbers.amount(item.getQty(), item.getUnitPrice())).reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    private void createItems(BizPurchaseIn header, PurchaseInCreateRequest request) {
        for (PurchaseInItemRequest input : request.getItems()) {
            BigDecimal rate = basicDataService.productUnitRate(input.getProductId(), input.getUnitId());
            BigDecimal baseQty = BusinessNumbers.baseQuantity(input.getQty(), rate);
            BizPurchaseInItem item = new BizPurchaseInItem();
            item.setPurchaseInId(header.getId()); item.setWarehouseId(header.getWarehouseId());
            item.setProductId(input.getProductId()); item.setUnitId(input.getUnitId());
            item.setQty(input.getQty()); item.setBaseQty(baseQty); item.setUnitPrice(input.getUnitPrice());
            item.setAmount(BusinessNumbers.amount(input.getQty(), input.getUnitPrice())); item.setRemark(input.getRemark());
            item.setBatchNo(input.getBatchNo()); item.setInboundTime(input.getInboundTime());
            item.setProductionDate(input.getProductionDate()); item.setExpiryDate(input.getExpiryDate());
            if (purchaseInItemMapper.insert(item) != 1) InventoryLedger.conflict();
            InvStock stock = ledger.inbound(header.getWarehouseId(), item.getProductId(), baseQty);
            InvBatch batch = new InvBatch();
            batch.setWarehouseId(header.getWarehouseId()); batch.setProductId(item.getProductId());
            batch.setSourceType("purchase_in"); batch.setSourceId(item.getId()); batch.setBatchNo(input.getBatchNo());
            batch.setInboundTime(input.getInboundTime()); batch.setProductionDate(input.getProductionDate());
            batch.setExpiryDate(input.getExpiryDate());
            batch.setUnitPrice(BusinessNumbers.quantity(input.getUnitPrice().divide(rate, 4, RoundingMode.HALF_UP), false));
            batch.setInitQty(baseQty); batch.setRemainQty(baseQty); batch.setStatus(1);
            if (invBatchMapper.insert(batch) != 1) InventoryLedger.conflict();
            ledger.flow(stock, batch.getId(), "purchase_in", header.getId(), item.getId(), baseQty,
                    input.getInboundTime(), "purchase inbound");
        }
    }

    private void rollbackItems(Long id) {
        List<BizPurchaseInItem> items = purchaseInItemMapper.selectList(new LambdaQueryWrapper<BizPurchaseInItem>()
                .eq(BizPurchaseInItem::getPurchaseInId, id).orderByAsc(BizPurchaseInItem::getId).last("FOR UPDATE"));
        for (BizPurchaseInItem item : items) {
            InvStock stock = ledger.requireStock(item.getWarehouseId(), item.getProductId());
            List<InvBatch> sourceBatches = invBatchMapper.selectList(new LambdaQueryWrapper<InvBatch>()
                    .eq(InvBatch::getSourceType, "purchase_in").eq(InvBatch::getSourceId, item.getId()).last("FOR UPDATE"));
            if (sourceBatches.size() != 1) InventoryLedger.conflict();
            InvBatch batch = sourceBatches.get(0);
            BigDecimal originalQty = item.getBaseQty() == null ? item.getQty() : item.getBaseQty();
            if (batch.getInitQty().compareTo(originalQty) != 0) InventoryLedger.conflict();
            if (batch.getRemainQty().compareTo(batch.getInitQty()) != 0
                    || invSaleBatchDeductionMapper.selectCount(new LambdaQueryWrapper<InvSaleBatchDeduction>()
                    .eq(InvSaleBatchDeduction::getBatchId, batch.getId())) > 0)
                throw new BusinessException(400, "purchase-in-already-consumed");
            ledger.change(stock, batch.getInitQty().negate());
            ledger.flow(stock, batch.getId(), "purchase_in_reverse", id, item.getId(), batch.getInitQty().negate(),
                    LocalDateTime.now(), "reverse purchase inbound");
            if (invBatchMapper.deleteById(batch.getId()) != 1) InventoryLedger.conflict();
        }
        purchaseInItemMapper.delete(new LambdaQueryWrapper<BizPurchaseInItem>().eq(BizPurchaseInItem::getPurchaseInId, id));
    }

    public PageResponse<PurchaseInDetailResponse> query(PurchaseInQueryRequest request) {
        Page<BizPurchaseIn> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<BizPurchaseIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(request.getOrderNo()), BizPurchaseIn::getOrderNo, request.getOrderNo())
                .eq(request.getWarehouseId() != null, BizPurchaseIn::getWarehouseId, request.getWarehouseId())
                .eq(request.getSupplierId() != null, BizPurchaseIn::getSupplierId, request.getSupplierId())
                .eq(request.getStatus() != null, BizPurchaseIn::getStatus, request.getStatus())
                .ge(request.getBizDateFrom() != null, BizPurchaseIn::getBizDate, request.getBizDateFrom())
                .le(request.getBizDateTo() != null, BizPurchaseIn::getBizDate, request.getBizDateTo())
                .orderByDesc(BizPurchaseIn::getId);
        Page<BizPurchaseIn> result = purchaseInMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords().stream().map(this::toResponseWithoutItems).toList(), result.getTotal());
    }

    public PurchaseInDetailResponse detail(Long id) {
        BizPurchaseIn purchaseIn = purchaseInMapper.selectById(id);
        if (purchaseIn == null) {
            throw new BusinessException(404, "purchase-in-not-found");
        }
        PurchaseInDetailResponse response = toResponseWithoutItems(purchaseIn);
        List<PurchaseInDetailResponse.Item> items = purchaseInItemMapper.selectList(
                        new LambdaQueryWrapper<BizPurchaseInItem>().eq(BizPurchaseInItem::getPurchaseInId, id))
                .stream()
                .map(this::toItemResponse)
                .toList();
        response.setItems(items);
        return response;
    }

    private PurchaseInDetailResponse toResponseWithoutItems(BizPurchaseIn purchaseIn) {
        PurchaseInDetailResponse response = new PurchaseInDetailResponse();
        response.setId(purchaseIn.getId());
        response.setOrderNo(purchaseIn.getOrderNo());
        response.setWarehouseId(purchaseIn.getWarehouseId());
        response.setWarehouseName(basicDataService.warehouseDetail(purchaseIn.getWarehouseId()).getWarehouseName());
        response.setSupplierId(purchaseIn.getSupplierId());
        response.setSupplierName(basicDataService.supplierDetail(purchaseIn.getSupplierId()).getSupplierName());
        response.setBizDate(purchaseIn.getBizDate());
        response.setTotalAmount(purchaseIn.getTotalAmount());
        response.setStatus(purchaseIn.getStatus());
        response.setRemark(purchaseIn.getRemark());
        response.setItems(List.of());
        return response;
    }

    private PurchaseInDetailResponse.Item toItemResponse(BizPurchaseInItem item) {
        PurchaseInDetailResponse.Item response = new PurchaseInDetailResponse.Item();
        response.setId(item.getId());
        response.setProductId(item.getProductId());
        response.setUnitId(item.getUnitId());
        response.setQty(item.getQty());
        response.setBaseQty(item.getBaseQty() == null ? item.getQty() : item.getBaseQty());
        var product = basicDataService.productDetail(item.getProductId());
        response.setProductName(product.getProductName());
        response.setSpec(product.getSpec());
        response.setCategoryId(product.getCategoryId());
        response.setCategoryName(product.getCategoryName());
        response.setUnitName(basicDataService.unitDetail(item.getUnitId()).getUnitName());
        response.setUnitPrice(item.getUnitPrice());
        response.setAmount(item.getAmount());
        response.setBatchNo(item.getBatchNo());
        response.setProductionDate(item.getProductionDate());
        response.setExpiryDate(item.getExpiryDate());
        response.setInboundTime(item.getInboundTime());
        response.setRemark(item.getRemark());
        return response;
    }
}
