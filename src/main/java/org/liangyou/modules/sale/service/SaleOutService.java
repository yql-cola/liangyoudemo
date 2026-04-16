package org.liangyou.modules.sale.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.PageResponse;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.inventory.entity.InvBatch;
import org.liangyou.modules.inventory.entity.InvSaleBatchDeduction;
import org.liangyou.modules.inventory.entity.InvStock;
import org.liangyou.modules.inventory.entity.InvStockFlow;
import org.liangyou.modules.inventory.mapper.InvBatchMapper;
import org.liangyou.modules.inventory.mapper.InvSaleBatchDeductionMapper;
import org.liangyou.modules.inventory.mapper.InvStockFlowMapper;
import org.liangyou.modules.inventory.mapper.InvStockMapper;
import org.liangyou.modules.sale.dto.SaleOutCreateRequest;
import org.liangyou.modules.sale.dto.SaleOutItemRequest;
import org.liangyou.modules.sale.dto.SaleOutQueryRequest;
import org.liangyou.modules.sale.entity.BizSaleOut;
import org.liangyou.modules.sale.entity.BizSaleOutItem;
import org.liangyou.modules.sale.mapper.BizSaleOutItemMapper;
import org.liangyou.modules.sale.mapper.BizSaleOutMapper;
import org.liangyou.modules.sale.vo.SaleOutDetailResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleOutService {

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final BizSaleOutMapper saleOutMapper;
    private final BizSaleOutItemMapper saleOutItemMapper;
    private final InvStockMapper invStockMapper;
    private final InvBatchMapper invBatchMapper;
    private final InvStockFlowMapper invStockFlowMapper;
    private final InvSaleBatchDeductionMapper invSaleBatchDeductionMapper;

    @Transactional
    public Long create(SaleOutCreateRequest request) {
        return createSaleOut(null, request);
    }

    @Transactional
    public void update(Long id, SaleOutCreateRequest request) {
        BizSaleOut saleOut = saleOutMapper.selectById(id);
        if (saleOut == null) {
            throw new BusinessException(404, "sale-out-not-found");
        }
        rollbackSaleOut(id);
        createSaleOut(saleOut.getOrderNo(), request);
    }

    @Transactional
    public void delete(Long id) {
        BizSaleOut saleOut = saleOutMapper.selectById(id);
        if (saleOut == null) {
            throw new BusinessException(404, "sale-out-not-found");
        }
        rollbackSaleOut(id);
    }

    private Long createSaleOut(String orderNo, SaleOutCreateRequest request) {
        BizSaleOut saleOut = new BizSaleOut();
        saleOut.setOrderNo(orderNo == null ? "SO" + LocalDateTime.now().format(ORDER_NO_FORMATTER) : orderNo);
        saleOut.setWarehouseId(request.getWarehouseId());
        saleOut.setCustomerId(request.getCustomerId());
        saleOut.setBizDate(request.getBizDate());
        saleOut.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        saleOut.setRemark(request.getRemark());
        saleOut.setTotalAmount(request.getItems().stream()
                .map(SaleOutItemRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        saleOutMapper.insert(saleOut);

        for (SaleOutItemRequest itemRequest : request.getItems()) {
            BizSaleOutItem item = new BizSaleOutItem();
            item.setSaleOutId(saleOut.getId());
            item.setWarehouseId(request.getWarehouseId());
            item.setProductId(itemRequest.getProductId());
            item.setUnitId(itemRequest.getUnitId());
            item.setQty(itemRequest.getQty());
            item.setUnitPrice(itemRequest.getUnitPrice());
            item.setAmount(itemRequest.getAmount());
            item.setRemark(itemRequest.getRemark());
            saleOutItemMapper.insert(item);

            InvStock stock = invStockMapper.selectOne(new LambdaQueryWrapper<InvStock>()
                    .eq(InvStock::getWarehouseId, request.getWarehouseId())
                    .eq(InvStock::getProductId, itemRequest.getProductId())
                    .last("limit 1"));
            if (stock == null || stock.getAvailableQty().compareTo(itemRequest.getQty()) < 0) {
                throw new BusinessException(400, "insufficient-stock");
            }

            BigDecimal remainToDeduct = itemRequest.getQty();
            BigDecimal afterQty = stock.getAvailableQty();
            List<InvBatch> batches = invBatchMapper.selectList(new LambdaQueryWrapper<InvBatch>()
                    .eq(InvBatch::getWarehouseId, request.getWarehouseId())
                    .eq(InvBatch::getProductId, itemRequest.getProductId())
                    .gt(InvBatch::getRemainQty, BigDecimal.ZERO)
                    .orderByAsc(InvBatch::getInboundTime)
                    .orderByAsc(InvBatch::getId));
            for (InvBatch batch : batches) {
                if (remainToDeduct.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                BigDecimal deductQty = batch.getRemainQty().min(remainToDeduct);
                batch.setRemainQty(batch.getRemainQty().subtract(deductQty));
                invBatchMapper.updateById(batch);
                remainToDeduct = remainToDeduct.subtract(deductQty);
                afterQty = afterQty.subtract(deductQty);

                InvSaleBatchDeduction deduction = new InvSaleBatchDeduction();
                deduction.setSaleOutId(saleOut.getId());
                deduction.setSaleOutItemId(item.getId());
                deduction.setBatchId(batch.getId());
                deduction.setDeductQty(deductQty);
                deduction.setUnitCost(batch.getUnitPrice());
                deduction.setAmount(batch.getUnitPrice().multiply(deductQty));
                invSaleBatchDeductionMapper.insert(deduction);

                InvStockFlow flow = new InvStockFlow();
                flow.setWarehouseId(request.getWarehouseId());
                flow.setProductId(itemRequest.getProductId());
                flow.setBatchId(batch.getId());
                flow.setFlowType("sale_out");
                flow.setBizType("sale_out");
                flow.setBizId(saleOut.getId());
                flow.setBizItemId(item.getId());
                flow.setChangeQty(deductQty.negate());
                flow.setAfterQty(afterQty);
                flow.setBizTime(LocalDateTime.now());
                flow.setRemark("sale outbound");
                invStockFlowMapper.insert(flow);
            }
            if (remainToDeduct.compareTo(BigDecimal.ZERO) > 0) {
                throw new BusinessException(400, "insufficient-stock");
            }

            stock.setTotalQty(stock.getTotalQty().subtract(itemRequest.getQty()));
            stock.setAvailableQty(stock.getAvailableQty().subtract(itemRequest.getQty()));
            invStockMapper.updateById(stock);
        }

        return saleOut.getId();
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
        response.setCustomerId(saleOut.getCustomerId());
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
        response.setUnitPrice(item.getUnitPrice());
        response.setAmount(item.getAmount());
        response.setRemark(item.getRemark());
        return response;
    }

    private void rollbackSaleOut(Long saleOutId) {
        List<BizSaleOutItem> items = saleOutItemMapper.selectList(
                new LambdaQueryWrapper<BizSaleOutItem>().eq(BizSaleOutItem::getSaleOutId, saleOutId));
        for (BizSaleOutItem item : items) {
            List<InvSaleBatchDeduction> deductions = invSaleBatchDeductionMapper.selectList(
                    new LambdaQueryWrapper<InvSaleBatchDeduction>().eq(InvSaleBatchDeduction::getSaleOutItemId, item.getId()));
            BigDecimal restoredQty = BigDecimal.ZERO;
            for (InvSaleBatchDeduction deduction : deductions) {
                InvBatch batch = invBatchMapper.selectById(deduction.getBatchId());
                if (batch == null) {
                    throw new BusinessException(400, "inventory-rollback-conflict");
                }
                batch.setRemainQty(batch.getRemainQty().add(deduction.getDeductQty()));
                invBatchMapper.updateById(batch);
                restoredQty = restoredQty.add(deduction.getDeductQty());
            }
            InvStock stock = invStockMapper.selectOne(new LambdaQueryWrapper<InvStock>()
                    .eq(InvStock::getWarehouseId, item.getWarehouseId())
                    .eq(InvStock::getProductId, item.getProductId())
                    .last("limit 1"));
            if (stock == null) {
                throw new BusinessException(400, "inventory-rollback-conflict");
            }
            stock.setTotalQty(stock.getTotalQty().add(restoredQty));
            stock.setAvailableQty(stock.getAvailableQty().add(restoredQty));
            invStockMapper.updateById(stock);
            invSaleBatchDeductionMapper.delete(new LambdaQueryWrapper<InvSaleBatchDeduction>()
                    .eq(InvSaleBatchDeduction::getSaleOutItemId, item.getId()));
            invStockFlowMapper.delete(new LambdaQueryWrapper<InvStockFlow>()
                    .eq(InvStockFlow::getBizType, "sale_out")
                    .eq(InvStockFlow::getBizId, saleOutId)
                    .eq(InvStockFlow::getBizItemId, item.getId()));
        }
        saleOutItemMapper.delete(new LambdaQueryWrapper<BizSaleOutItem>().eq(BizSaleOutItem::getSaleOutId, saleOutId));
        saleOutMapper.deleteById(saleOutId);
    }
}
