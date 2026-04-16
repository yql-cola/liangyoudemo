package org.liangyou.modules.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.PageResponse;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.inventory.dto.InventoryFlowQueryRequest;
import org.liangyou.modules.inventory.dto.InventoryQueryRequest;
import org.liangyou.modules.inventory.dto.InventoryUpdateRequest;
import org.liangyou.modules.inventory.entity.InvBatch;
import org.liangyou.modules.inventory.entity.InvStock;
import org.liangyou.modules.inventory.entity.InvStockFlow;
import org.liangyou.modules.inventory.mapper.InvBatchMapper;
import org.liangyou.modules.inventory.mapper.InvStockFlowMapper;
import org.liangyou.modules.inventory.mapper.InvStockMapper;
import org.liangyou.modules.inventory.vo.InventoryBatchResponse;
import org.liangyou.modules.inventory.vo.InventoryFlowResponse;
import org.liangyou.modules.inventory.vo.InventoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InvStockMapper invStockMapper;
    private final InvBatchMapper invBatchMapper;
    private final InvStockFlowMapper invStockFlowMapper;

    public PageResponse<InventoryResponse> query(InventoryQueryRequest request) {
        Page<InvStock> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<InvStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getWarehouseId() != null, InvStock::getWarehouseId, request.getWarehouseId())
                .eq(request.getProductId() != null, InvStock::getProductId, request.getProductId())
                .orderByDesc(InvStock::getId);
        Page<InvStock> result = invStockMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords().stream().map(this::toInventoryResponse).toList(), result.getTotal());
    }

    public List<InventoryBatchResponse> batches(Long warehouseId, Long productId) {
        return invBatchMapper.selectList(new LambdaQueryWrapper<InvBatch>()
                        .eq(warehouseId != null, InvBatch::getWarehouseId, warehouseId)
                        .eq(productId != null, InvBatch::getProductId, productId)
                        .orderByAsc(InvBatch::getInboundTime)
                        .orderByAsc(InvBatch::getId))
                .stream()
                .map(this::toBatchResponse)
                .toList();
    }

    public List<InventoryFlowResponse> flows(InventoryFlowQueryRequest request) {
        return invStockFlowMapper.selectList(new LambdaQueryWrapper<InvStockFlow>()
                        .eq(request.getWarehouseId() != null, InvStockFlow::getWarehouseId, request.getWarehouseId())
                        .eq(request.getProductId() != null, InvStockFlow::getProductId, request.getProductId())
                        .ge(request.getBizTimeFrom() != null, InvStockFlow::getBizTime, request.getBizTimeFrom())
                        .le(request.getBizTimeTo() != null, InvStockFlow::getBizTime, request.getBizTimeTo())
                        .orderByDesc(InvStockFlow::getBizTime)
                        .orderByDesc(InvStockFlow::getId))
                .stream()
                .map(this::toFlowResponse)
                .toList();
    }

    @Transactional
    public void update(Long id, InventoryUpdateRequest request) {
        InvStock stock = invStockMapper.selectById(id);
        if (stock == null) {
            throw new BusinessException(404, "inventory-not-found");
        }
        BigDecimal changeQty = request.getAvailableQty().subtract(stock.getAvailableQty());
        stock.setTotalQty(request.getTotalQty());
        stock.setAvailableQty(request.getAvailableQty());
        invStockMapper.updateById(stock);

        InvStockFlow flow = new InvStockFlow();
        flow.setWarehouseId(stock.getWarehouseId());
        flow.setProductId(stock.getProductId());
        flow.setFlowType("adjust");
        flow.setBizType("inventory_adjust");
        flow.setBizId(stock.getId());
        flow.setChangeQty(changeQty);
        flow.setAfterQty(stock.getAvailableQty());
        flow.setBizTime(LocalDateTime.now());
        flow.setRemark(request.getRemark());
        invStockFlowMapper.insert(flow);
    }

    @Transactional
    public void delete(Long id) {
        InvStock stock = invStockMapper.selectById(id);
        if (stock == null) {
            throw new BusinessException(404, "inventory-not-found");
        }
        invStockMapper.deleteById(id);
    }

    private InventoryResponse toInventoryResponse(InvStock stock) {
        InventoryResponse response = new InventoryResponse();
        response.setId(stock.getId());
        response.setWarehouseId(stock.getWarehouseId());
        response.setProductId(stock.getProductId());
        response.setTotalQty(stock.getTotalQty());
        response.setLockedQty(stock.getLockedQty());
        response.setAvailableQty(stock.getAvailableQty());
        return response;
    }

    private InventoryBatchResponse toBatchResponse(InvBatch batch) {
        InventoryBatchResponse response = new InventoryBatchResponse();
        response.setId(batch.getId());
        response.setWarehouseId(batch.getWarehouseId());
        response.setProductId(batch.getProductId());
        response.setBatchNo(batch.getBatchNo());
        response.setInboundTime(batch.getInboundTime());
        response.setProductionDate(batch.getProductionDate());
        response.setExpiryDate(batch.getExpiryDate());
        response.setUnitPrice(batch.getUnitPrice());
        response.setInitQty(batch.getInitQty());
        response.setRemainQty(batch.getRemainQty());
        return response;
    }

    private InventoryFlowResponse toFlowResponse(InvStockFlow flow) {
        InventoryFlowResponse response = new InventoryFlowResponse();
        response.setId(flow.getId());
        response.setWarehouseId(flow.getWarehouseId());
        response.setProductId(flow.getProductId());
        response.setBatchId(flow.getBatchId());
        response.setFlowType(flow.getFlowType());
        response.setBizType(flow.getBizType());
        response.setBizId(flow.getBizId());
        response.setBizItemId(flow.getBizItemId());
        response.setChangeQty(flow.getChangeQty());
        response.setAfterQty(flow.getAfterQty());
        response.setBizTime(flow.getBizTime());
        response.setRemark(flow.getRemark());
        return response;
    }
}
