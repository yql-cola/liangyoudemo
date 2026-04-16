package org.liangyou.modules.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.PageResponse;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.inventory.entity.InvBatch;
import org.liangyou.modules.inventory.entity.InvStock;
import org.liangyou.modules.inventory.entity.InvStockFlow;
import org.liangyou.modules.inventory.mapper.InvBatchMapper;
import org.liangyou.modules.inventory.mapper.InvStockFlowMapper;
import org.liangyou.modules.inventory.mapper.InvStockMapper;
import org.liangyou.modules.purchase.dto.PurchaseInCreateRequest;
import org.liangyou.modules.purchase.dto.PurchaseInItemRequest;
import org.liangyou.modules.purchase.dto.PurchaseInQueryRequest;
import org.liangyou.modules.purchase.entity.BizPurchaseIn;
import org.liangyou.modules.purchase.entity.BizPurchaseInItem;
import org.liangyou.modules.purchase.mapper.BizPurchaseInItemMapper;
import org.liangyou.modules.purchase.mapper.BizPurchaseInMapper;
import org.liangyou.modules.purchase.vo.PurchaseInDetailResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseInService {

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final BizPurchaseInMapper purchaseInMapper;
    private final BizPurchaseInItemMapper purchaseInItemMapper;
    private final InvStockMapper invStockMapper;
    private final InvBatchMapper invBatchMapper;
    private final InvStockFlowMapper invStockFlowMapper;

    @Transactional
    public Long create(PurchaseInCreateRequest request) {
        return createPurchaseIn(null, request);
    }

    @Transactional
    public void update(Long id, PurchaseInCreateRequest request) {
        BizPurchaseIn purchaseIn = purchaseInMapper.selectById(id);
        if (purchaseIn == null) {
            throw new BusinessException(404, "purchase-in-not-found");
        }
        rollbackPurchaseIn(id);
        createPurchaseIn(purchaseIn.getOrderNo(), request);
    }

    @Transactional
    public void delete(Long id) {
        BizPurchaseIn purchaseIn = purchaseInMapper.selectById(id);
        if (purchaseIn == null) {
            throw new BusinessException(404, "purchase-in-not-found");
        }
        rollbackPurchaseIn(id);
    }

    private Long createPurchaseIn(String orderNo, PurchaseInCreateRequest request) {
        BizPurchaseIn purchaseIn = new BizPurchaseIn();
        purchaseIn.setOrderNo(orderNo == null ? "PI" + LocalDateTime.now().format(ORDER_NO_FORMATTER) : orderNo);
        purchaseIn.setWarehouseId(request.getWarehouseId());
        purchaseIn.setSupplierId(request.getSupplierId());
        purchaseIn.setBizDate(request.getBizDate());
        purchaseIn.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        purchaseIn.setRemark(request.getRemark());
        purchaseIn.setTotalAmount(request.getItems().stream()
                .map(PurchaseInItemRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        purchaseInMapper.insert(purchaseIn);

        for (PurchaseInItemRequest itemRequest : request.getItems()) {
            BizPurchaseInItem item = new BizPurchaseInItem();
            item.setPurchaseInId(purchaseIn.getId());
            item.setWarehouseId(request.getWarehouseId());
            item.setProductId(itemRequest.getProductId());
            item.setUnitId(itemRequest.getUnitId());
            item.setQty(itemRequest.getQty());
            item.setUnitPrice(itemRequest.getUnitPrice());
            item.setAmount(itemRequest.getAmount());
            item.setBatchNo(itemRequest.getBatchNo());
            item.setProductionDate(itemRequest.getProductionDate());
            item.setExpiryDate(itemRequest.getExpiryDate());
            item.setInboundTime(itemRequest.getInboundTime());
            item.setRemark(itemRequest.getRemark());
            purchaseInItemMapper.insert(item);

            InvStock stock = upsertStock(request.getWarehouseId(), itemRequest.getProductId(), itemRequest.getQty());

            InvBatch batch = new InvBatch();
            batch.setWarehouseId(request.getWarehouseId());
            batch.setProductId(itemRequest.getProductId());
            batch.setSourceType("purchase_in");
            batch.setSourceId(item.getId());
            batch.setBatchNo(itemRequest.getBatchNo());
            batch.setInboundTime(itemRequest.getInboundTime());
            batch.setProductionDate(itemRequest.getProductionDate());
            batch.setExpiryDate(itemRequest.getExpiryDate());
            batch.setUnitPrice(itemRequest.getUnitPrice());
            batch.setInitQty(itemRequest.getQty());
            batch.setRemainQty(itemRequest.getQty());
            batch.setStatus(1);
            invBatchMapper.insert(batch);

            InvStockFlow flow = new InvStockFlow();
            flow.setWarehouseId(request.getWarehouseId());
            flow.setProductId(itemRequest.getProductId());
            flow.setBatchId(batch.getId());
            flow.setFlowType("purchase_in");
            flow.setBizType("purchase_in");
            flow.setBizId(purchaseIn.getId());
            flow.setBizItemId(item.getId());
            flow.setChangeQty(itemRequest.getQty());
            flow.setAfterQty(stock.getAvailableQty());
            flow.setBizTime(itemRequest.getInboundTime());
            flow.setRemark("purchase inbound");
            invStockFlowMapper.insert(flow);
        }

        return purchaseIn.getId();
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

    private InvStock upsertStock(Long warehouseId, Long productId, BigDecimal qty) {
        InvStock stock = invStockMapper.selectOne(new LambdaQueryWrapper<InvStock>()
                .eq(InvStock::getWarehouseId, warehouseId)
                .eq(InvStock::getProductId, productId)
                .last("limit 1"));
        if (stock == null) {
            stock = new InvStock();
            stock.setWarehouseId(warehouseId);
            stock.setProductId(productId);
            stock.setLockedQty(BigDecimal.ZERO);
            stock.setTotalQty(qty);
            stock.setAvailableQty(qty);
            invStockMapper.insert(stock);
            return stock;
        }
        stock.setTotalQty(stock.getTotalQty().add(qty));
        stock.setAvailableQty(stock.getAvailableQty().add(qty));
        invStockMapper.updateById(stock);
        return stock;
    }

    private void rollbackPurchaseIn(Long purchaseInId) {
        BizPurchaseIn purchaseIn = purchaseInMapper.selectById(purchaseInId);
        List<BizPurchaseInItem> items = purchaseInItemMapper.selectList(
                new LambdaQueryWrapper<BizPurchaseInItem>().eq(BizPurchaseInItem::getPurchaseInId, purchaseInId));
        for (BizPurchaseInItem item : items) {
            InvBatch batch = invBatchMapper.selectOne(new LambdaQueryWrapper<InvBatch>()
                    .eq(InvBatch::getSourceType, "purchase_in")
                    .eq(InvBatch::getSourceId, item.getId())
                    .last("limit 1"));
            if (batch != null) {
                if (batch.getRemainQty().compareTo(batch.getInitQty()) != 0) {
                    throw new BusinessException(400, "purchase-in-already-consumed");
                }
                invBatchMapper.deleteById(batch.getId());
            }
            InvStock stock = invStockMapper.selectOne(new LambdaQueryWrapper<InvStock>()
                    .eq(InvStock::getWarehouseId, item.getWarehouseId())
                    .eq(InvStock::getProductId, item.getProductId())
                    .last("limit 1"));
            if (stock == null || stock.getAvailableQty().compareTo(item.getQty()) < 0) {
                throw new BusinessException(400, "inventory-rollback-conflict");
            }
            stock.setTotalQty(stock.getTotalQty().subtract(item.getQty()));
            stock.setAvailableQty(stock.getAvailableQty().subtract(item.getQty()));
            invStockMapper.updateById(stock);
            invStockFlowMapper.delete(new LambdaQueryWrapper<InvStockFlow>()
                    .eq(InvStockFlow::getBizType, "purchase_in")
                    .eq(InvStockFlow::getBizId, purchaseInId)
                    .eq(InvStockFlow::getBizItemId, item.getId()));
        }
        purchaseInItemMapper.delete(new LambdaQueryWrapper<BizPurchaseInItem>().eq(BizPurchaseInItem::getPurchaseInId, purchaseInId));
        purchaseInMapper.deleteById(purchaseInId);
        if (purchaseIn == null) {
            throw new BusinessException(404, "purchase-in-not-found");
        }
    }

    private PurchaseInDetailResponse toResponseWithoutItems(BizPurchaseIn purchaseIn) {
        PurchaseInDetailResponse response = new PurchaseInDetailResponse();
        response.setId(purchaseIn.getId());
        response.setOrderNo(purchaseIn.getOrderNo());
        response.setWarehouseId(purchaseIn.getWarehouseId());
        response.setSupplierId(purchaseIn.getSupplierId());
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
