package org.liangyou.modules.sale.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.PageResponse;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.inventory.entity.*;
import org.liangyou.modules.inventory.mapper.*;
import org.liangyou.modules.inventory.service.*;
import org.liangyou.modules.sale.dto.*;
import org.liangyou.modules.sale.entity.*;
import org.liangyou.modules.sale.mapper.*;
import org.liangyou.modules.sale.vo.SaleOutDetailResponse;
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
public class SaleOutService {
    private final BizSaleOutMapper saleOutMapper;
    private final BizSaleOutItemMapper saleOutItemMapper;
    private final InvBatchMapper invBatchMapper;
    private final InvSaleBatchDeductionMapper invSaleBatchDeductionMapper;
    private final BasicDataService basicDataService;
    private final WarehouseMutationLock locks;
    private final InventoryLedger ledger;

    @Transactional
    public Long create(SaleOutCreateRequest request) {
        requireRequest(request);
        locks.lock(request.getWarehouseId());
        validateMasterData(request);
        BizSaleOut header = new BizSaleOut();
        header.setOrderNo("SO" + UUID.randomUUID().toString().replace("-", ""));
        applyHeader(header, request);
        if (saleOutMapper.insert(header) != 1) InventoryLedger.conflict();
        createItems(header, request);
        return header.getId();
    }

    @Transactional
    public void update(Long id, SaleOutCreateRequest request) {
        requireRequest(request);
        BizSaleOut header = requireHeader(id);
        locks.lock(header.getWarehouseId(), request.getWarehouseId());
        validateMasterData(request);
        rollbackItems(id);
        applyHeader(header, request);
        if (saleOutMapper.updateById(header) != 1) InventoryLedger.conflict();
        createItems(header, request);
    }

    @Transactional
    public void delete(Long id) {
        BizSaleOut header = requireHeader(id);
        locks.lock(header.getWarehouseId());
        rollbackItems(id);
        if (saleOutMapper.deleteById(id) != 1) InventoryLedger.conflict();
    }

    private BizSaleOut requireHeader(Long id) {
        BizSaleOut header = saleOutMapper.selectOne(new LambdaQueryWrapper<BizSaleOut>()
                .eq(BizSaleOut::getId, id).last("FOR UPDATE"));
        if (header == null) throw new BusinessException(404, "sale-out-not-found");
        return header;
    }

    private void requireRequest(SaleOutCreateRequest request) {
        if (request == null || request.getWarehouseId() == null || request.getCustomerId() == null
                || request.getBizDate() == null || request.getItems() == null || request.getItems().isEmpty())
            throw new BusinessException(400, "invalid-business-request");
        for (SaleOutItemRequest item : request.getItems()) {
            if (item == null || item.getProductId() == null || item.getUnitId() == null)
                throw new BusinessException(400, "invalid-business-request");
            BusinessNumbers.quantity(item.getQty(), true);
            BusinessNumbers.quantity(item.getUnitPrice(), false);
            if (item.getAmount() != null && item.getAmount().signum() < 0)
                throw new BusinessException(400, "invalid-business-number");
        }
    }

    private void validateMasterData(SaleOutCreateRequest request) {
        basicDataService.validateSaleMasterData(request.getWarehouseId(), request.getCustomerId(),
                request.getItems().stream().map(SaleOutItemRequest::getProductId).toList(),
                request.getItems().stream().map(SaleOutItemRequest::getUnitId).toList());
    }

    private void applyHeader(BizSaleOut header, SaleOutCreateRequest request) {
        header.setWarehouseId(request.getWarehouseId()); header.setCustomerId(request.getCustomerId());
        header.setBizDate(request.getBizDate()); header.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        header.setRemark(request.getRemark());
        header.setTotalAmount(BusinessNumbers.money(request.getItems().stream()
                .map(item -> BusinessNumbers.amount(item.getQty(), item.getUnitPrice())).reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    private void createItems(BizSaleOut header, SaleOutCreateRequest request) {
        for (SaleOutItemRequest input : request.getItems()) {
            BigDecimal rate = basicDataService.productUnitRate(input.getProductId(), input.getUnitId());
            BigDecimal baseQty = BusinessNumbers.baseQuantity(input.getQty(), rate);
            BizSaleOutItem item = new BizSaleOutItem();
            item.setSaleOutId(header.getId()); item.setWarehouseId(header.getWarehouseId());
            item.setProductId(input.getProductId()); item.setUnitId(input.getUnitId());
            item.setQty(input.getQty()); item.setBaseQty(baseQty); item.setUnitPrice(input.getUnitPrice());
            item.setAmount(BusinessNumbers.amount(input.getQty(), input.getUnitPrice())); item.setRemark(input.getRemark());
            if (saleOutItemMapper.insert(item) != 1) InventoryLedger.conflict();
            InvStock stock = ledger.stock(header.getWarehouseId(), item.getProductId());
            if (stock == null || stock.getAvailableQty().compareTo(baseQty) < 0)
                throw new BusinessException(400, "insufficient-stock");
            BigDecimal remaining = baseQty;
            for (InvBatch batch : ledger.fifo(header.getWarehouseId(), item.getProductId())) {
                if (remaining.signum() == 0) break;
                BigDecimal deducted = remaining.min(batch.getRemainQty());
                batch.setRemainQty(batch.getRemainQty().subtract(deducted)); ledger.saveBatch(batch);
                ledger.change(stock, deducted.negate()); remaining = remaining.subtract(deducted);
                InvSaleBatchDeduction deduction = new InvSaleBatchDeduction();
                deduction.setSaleOutId(header.getId()); deduction.setSaleOutItemId(item.getId());
                deduction.setBatchId(batch.getId()); deduction.setDeductQty(deducted); deduction.setUnitCost(batch.getUnitPrice());
                deduction.setAmount(BusinessNumbers.money(batch.getUnitPrice().multiply(deducted)));
                if (invSaleBatchDeductionMapper.insert(deduction) != 1) InventoryLedger.conflict();
                ledger.flow(stock, batch.getId(), "sale_out", header.getId(), item.getId(), deducted.negate(),
                        LocalDateTime.now(), "sale outbound");
            }
            if (remaining.signum() > 0) throw new BusinessException(400, "insufficient-stock");
        }
    }

    private void rollbackItems(Long id) {
        List<BizSaleOutItem> items = saleOutItemMapper.selectList(new LambdaQueryWrapper<BizSaleOutItem>()
                .eq(BizSaleOutItem::getSaleOutId, id).orderByAsc(BizSaleOutItem::getId).last("FOR UPDATE"));
        for (BizSaleOutItem item : items) {
            InvStock stock = ledger.requireStock(item.getWarehouseId(), item.getProductId());
            List<InvSaleBatchDeduction> deductions = invSaleBatchDeductionMapper.selectList(
                    new LambdaQueryWrapper<InvSaleBatchDeduction>().eq(InvSaleBatchDeduction::getSaleOutItemId, item.getId())
                            .orderByAsc(InvSaleBatchDeduction::getId).last("FOR UPDATE"));
            BigDecimal originalQty = item.getBaseQty() == null ? item.getQty() : item.getBaseQty();
            BigDecimal restored = deductions.stream().map(InvSaleBatchDeduction::getDeductQty).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (deductions.isEmpty() || restored.compareTo(originalQty) != 0) InventoryLedger.conflict();
            for (InvSaleBatchDeduction deduction : deductions) {
                InvBatch batch = invBatchMapper.selectOne(new LambdaQueryWrapper<InvBatch>()
                        .eq(InvBatch::getId, deduction.getBatchId()).last("FOR UPDATE"));
                if (batch == null || deduction.getDeductQty().signum() <= 0
                        || !item.getWarehouseId().equals(batch.getWarehouseId()) || !item.getProductId().equals(batch.getProductId()))
                    InventoryLedger.conflict();
                batch.setRemainQty(batch.getRemainQty().add(deduction.getDeductQty())); ledger.saveBatch(batch);
                ledger.change(stock, deduction.getDeductQty());
                ledger.flow(stock, batch.getId(), "sale_out_reverse", id, item.getId(), deduction.getDeductQty(),
                        LocalDateTime.now(), "reverse sale outbound");
            }
            invSaleBatchDeductionMapper.delete(new LambdaQueryWrapper<InvSaleBatchDeduction>()
                    .eq(InvSaleBatchDeduction::getSaleOutItemId, item.getId()));
        }
        saleOutItemMapper.delete(new LambdaQueryWrapper<BizSaleOutItem>().eq(BizSaleOutItem::getSaleOutId, id));
    }

    public PageResponse<SaleOutDetailResponse> query(SaleOutQueryRequest request) {
        Page<BizSaleOut> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<BizSaleOut> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(request.getOrderNo()), BizSaleOut::getOrderNo, request.getOrderNo())
                .eq(request.getWarehouseId() != null, BizSaleOut::getWarehouseId, request.getWarehouseId())
                .eq(request.getCustomerId() != null, BizSaleOut::getCustomerId, request.getCustomerId())
                .eq(request.getStatus() != null, BizSaleOut::getStatus, request.getStatus())
                .ge(request.getBizDateFrom() != null, BizSaleOut::getBizDate, request.getBizDateFrom())
                .le(request.getBizDateTo() != null, BizSaleOut::getBizDate, request.getBizDateTo())
                .orderByDesc(BizSaleOut::getId);
        Page<BizSaleOut> result = saleOutMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords().stream().map(this::toResponseWithoutItems).toList(), result.getTotal());
    }

    public SaleOutDetailResponse detail(Long id) {
        BizSaleOut saleOut = saleOutMapper.selectById(id);
        if (saleOut == null) {
            throw new BusinessException(404, "sale-out-not-found");
        }
        SaleOutDetailResponse response = toResponseWithoutItems(saleOut);
        List<SaleOutDetailResponse.Item> items = saleOutItemMapper.selectList(
                        new LambdaQueryWrapper<BizSaleOutItem>().eq(BizSaleOutItem::getSaleOutId, id))
                .stream()
                .map(this::toItemResponse)
                .toList();
        response.setItems(items);
        return response;
    }

    private SaleOutDetailResponse toResponseWithoutItems(BizSaleOut saleOut) {
        SaleOutDetailResponse response = new SaleOutDetailResponse();
        response.setId(saleOut.getId());
        response.setOrderNo(saleOut.getOrderNo());
        response.setWarehouseId(saleOut.getWarehouseId());
        response.setWarehouseName(basicDataService.warehouseDetail(saleOut.getWarehouseId()).getWarehouseName());
        response.setCustomerId(saleOut.getCustomerId());
        response.setCustomerName(basicDataService.customerDetail(saleOut.getCustomerId()).getCustomerName());
        response.setBizDate(saleOut.getBizDate());
        response.setTotalAmount(saleOut.getTotalAmount());
        response.setStatus(saleOut.getStatus());
        response.setRemark(saleOut.getRemark());
        response.setItems(List.of());
        return response;
    }

    private SaleOutDetailResponse.Item toItemResponse(BizSaleOutItem item) {
        SaleOutDetailResponse.Item response = new SaleOutDetailResponse.Item();
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
        response.setRemark(item.getRemark());
        return response;
    }


}
