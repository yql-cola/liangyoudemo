package org.liangyou.modules.sale.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.PageResponse;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.basic.entity.BaseCustomer;
import org.liangyou.modules.inventory.entity.*;
import org.liangyou.modules.inventory.mapper.*;
import org.liangyou.modules.inventory.service.*;
import org.liangyou.modules.sale.dto.*;
import org.liangyou.modules.sale.entity.*;
import org.liangyou.modules.sale.mapper.*;
import org.liangyou.modules.sale.vo.SaleOutDetailResponse;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.liangyou.security.AuthenticatedUserPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
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
    private final JdbcTemplate jdbc;

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
        requireStatus(header, "draft", "pending_handoff", "pending_delivery");
        if (!"draft".equals(header.getOrderStatus()) && !header.getFulfillmentType().equals(request.getFulfillmentType()))
            throw new BusinessException(409, "sale-order-fulfillment-locked");
        locks.lock(header.getWarehouseId(), request.getWarehouseId());
        validateMasterData(request);
        boolean confirmed = !"draft".equals(header.getOrderStatus());
        if (confirmed) releaseReservations(header);
        saleOutItemMapper.delete(new LambdaQueryWrapper<BizSaleOutItem>().eq(BizSaleOutItem::getSaleOutId, id));
        applyHeader(header, request);
        if (saleOutMapper.updateById(header) != 1) InventoryLedger.conflict();
        createItems(header, request);
        if (confirmed) reserveItems(header);
    }

    @Transactional
    public void delete(Long id) {
        BizSaleOut header = requireHeader(id);
        requireStatus(header, "draft");
        locks.lock(header.getWarehouseId());
        saleOutItemMapper.delete(new LambdaQueryWrapper<BizSaleOutItem>().eq(BizSaleOutItem::getSaleOutId, id));
        if (saleOutMapper.deleteById(id) != 1) InventoryLedger.conflict();
    }

    @Transactional
    public void confirm(Long id) {
        BizSaleOut header = requireHeader(id);
        requireStatus(header, "draft");
        locks.lock(header.getWarehouseId());
        reserveItems(header);
        transition(header, "pickup".equals(header.getFulfillmentType()) ? "pending_handoff" : "pending_delivery", "confirm", null);
    }

    @Transactional
    public void handoff(Long id) { complete(id, "pending_handoff", "handoff"); }

    @Transactional
    public void completeDelivery(Long id) { complete(id, "in_delivery", "delivered"); }

    @Transactional
    public void startDelivery(Long id) {
        BizSaleOut header = requireHeader(id);
        requireStatus(header, "pending_delivery");
        transition(header, "in_delivery", "delivery_started", null);
    }

    @Transactional
    public void deliveryFailed(Long id, String reason) {
        BizSaleOut header = requireHeader(id);
        requireStatus(header, "in_delivery");
        if (!StringUtils.hasText(reason)) throw new BusinessException(400, "delivery-reason-required");
        transition(header, "awaiting_return", "delivery_failed", reason);
    }

    @Transactional
    public void returned(Long id) {
        BizSaleOut header = requireHeader(id);
        requireStatus(header, "awaiting_return");
        locks.lock(header.getWarehouseId());
        releaseReservations(header);
        transition(header, "returned", "returned", null);
    }

    @Transactional
    public void cancel(Long id) {
        BizSaleOut header = requireHeader(id);
        requireStatus(header, "draft", "pending_handoff", "pending_delivery");
        locks.lock(header.getWarehouseId());
        if (!"draft".equals(header.getOrderStatus())) releaseReservations(header);
        transition(header, "cancelled", "cancelled", null);
    }

    private void complete(Long id, String expected, String event) {
        BizSaleOut header = requireHeader(id);
        requireStatus(header, expected);
        locks.lock(header.getWarehouseId());
        List<BizSaleOutItem> items = items(id);
        for (BizSaleOutItem item : items) {
            BigDecimal qty;
            try {
                qty = jdbc.queryForObject("SELECT base_qty FROM sale_order_reservation WHERE sale_out_item_id=? AND status='active' FOR UPDATE",
                        BigDecimal.class, item.getId());
            } catch (EmptyResultDataAccessException ex) {
                InventoryLedger.conflict();
                return;
            }
            if (qty == null || qty.compareTo(item.getBaseQty()) != 0) InventoryLedger.conflict();
            InvStock stock = ledger.requireStock(item.getWarehouseId(), item.getProductId());
            BigDecimal batchTotal = ledger.fifo(item.getWarehouseId(), item.getProductId()).stream()
                    .map(InvBatch::getRemainQty).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (batchTotal.compareTo(stock.getTotalQty()) != 0) InventoryLedger.conflict();
            BigDecimal remaining = qty;
            for (InvBatch batch : ledger.fifo(item.getWarehouseId(), item.getProductId())) {
                if (remaining.signum() == 0) break;
                BigDecimal deducted = remaining.min(batch.getRemainQty());
                batch.setRemainQty(batch.getRemainQty().subtract(deducted)); ledger.saveBatch(batch);
                remaining = remaining.subtract(deducted);
                InvSaleBatchDeduction deduction = new InvSaleBatchDeduction();
                deduction.setSaleOutId(id); deduction.setSaleOutItemId(item.getId()); deduction.setBatchId(batch.getId());
                deduction.setDeductQty(deducted); deduction.setUnitCost(batch.getUnitPrice());
                deduction.setAmount(BusinessNumbers.money(batch.getUnitPrice().multiply(deducted)));
                if (invSaleBatchDeductionMapper.insert(deduction) != 1) InventoryLedger.conflict();
                ledger.consume(stock, deducted);
                ledger.flow(stock, batch.getId(), "sale_out", id, item.getId(), deducted.negate(), LocalDateTime.now(), "sale outbound");
            }
            if (remaining.signum() != 0) InventoryLedger.conflict();
            jdbc.update("UPDATE sale_order_reservation SET status='consumed' WHERE sale_out_item_id=? AND status='active'", item.getId());
        }
        header.setCompletedTime(LocalDateTime.now());
        transition(header, "completed", event, null);
    }

    private void reserveItems(BizSaleOut header) {
        for (BizSaleOutItem item : items(header.getId())) {
            InvStock stock = ledger.stock(header.getWarehouseId(), item.getProductId());
            if (stock == null) throw new BusinessException(400, "insufficient-stock");
            ledger.reserve(stock, item.getBaseQty());
            jdbc.update("INSERT INTO sale_order_reservation(sale_out_id,sale_out_item_id,warehouse_id,product_id,base_qty,status) VALUES(?,?,?,?,?,'active')",
                    header.getId(), item.getId(), header.getWarehouseId(), item.getProductId(), item.getBaseQty());
        }
    }

    private void releaseReservations(BizSaleOut header) {
        record Reserved(Long id, Long warehouseId, Long productId, BigDecimal qty) {}
        List<Reserved> active = jdbc.query("SELECT id,warehouse_id,product_id,base_qty FROM sale_order_reservation WHERE sale_out_id=? AND status='active' FOR UPDATE",
                (rs, row) -> new Reserved(rs.getLong("id"), rs.getLong("warehouse_id"), rs.getLong("product_id"), rs.getBigDecimal("base_qty")), header.getId());
        for (Reserved reservation : active) {
            InvStock stock = ledger.requireStock(reservation.warehouseId(), reservation.productId());
            ledger.release(stock, reservation.qty());
            jdbc.update("UPDATE sale_order_reservation SET status='released' WHERE id=?", reservation.id());
        }
    }

    private List<BizSaleOutItem> items(Long id) {
        return saleOutItemMapper.selectList(new LambdaQueryWrapper<BizSaleOutItem>()
                .eq(BizSaleOutItem::getSaleOutId, id).orderByAsc(BizSaleOutItem::getId).last("FOR UPDATE"));
    }

    private void requireStatus(BizSaleOut header, String... allowed) {
        if (!java.util.List.of(allowed).contains(header.getOrderStatus())) throw new BusinessException(409, "sale-order-state-conflict");
    }

    private void transition(BizSaleOut header, String state, String event, String detail) {
        header.setOrderStatus(state);
        if (saleOutMapper.updateById(header) != 1) InventoryLedger.conflict();
        recordEvent(header.getId(), event, detail);
    }

    @Transactional
    public void recordDocumentRequest(Long id, String event) {
        requireHeader(id);
        recordEvent(id, event, "template=v1");
    }

    private void recordEvent(Long id, String event, String detail) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = principal instanceof AuthenticatedUserPrincipal user ? user.getId() : null;
        jdbc.update("INSERT INTO sale_order_event(sale_out_id,event_type,operator_id,detail) VALUES(?,?,?,?)",
                id, event, userId, detail);
    }

    private BizSaleOut requireHeader(Long id) {
        BizSaleOut header = saleOutMapper.selectOne(new LambdaQueryWrapper<BizSaleOut>()
                .eq(BizSaleOut::getId, id).last("FOR UPDATE"));
        if (header == null) throw new BusinessException(404, "sale-out-not-found");
        return header;
    }

    private void requireRequest(SaleOutCreateRequest request) {
        if (request == null || request.getWarehouseId() == null
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
        if (!java.util.Set.of("retail", "wholesale").contains(request.getSaleType())
                || !java.util.Set.of("pickup", "delivery").contains(request.getFulfillmentType()))
            throw new BusinessException(400, "sale-order-type-invalid");
        BaseCustomer customer = request.getCustomerId() == null ? null : basicDataService.customerDetail(request.getCustomerId());
        header.setWarehouseId(request.getWarehouseId()); header.setCustomerId(request.getCustomerId());
        header.setBizDate(request.getBizDate()); header.setStatus(1);
        header.setOrderStatus(header.getOrderStatus() == null ? "draft" : header.getOrderStatus());
        header.setSaleType(request.getSaleType()); header.setFulfillmentType(request.getFulfillmentType());
        header.setCustomerNameSnapshot(StringUtils.hasText(request.getCustomerName()) ? request.getCustomerName().trim()
                : customer == null ? "散客" : customer.getCustomerName());
        header.setContactNameSnapshot(StringUtils.hasText(request.getContactName()) ? request.getContactName()
                : customer == null ? null : customer.getContactName());
        header.setContactPhoneSnapshot(StringUtils.hasText(request.getContactPhone()) ? request.getContactPhone()
                : customer == null ? null : customer.getContactPhone());
        header.setDeliveryAddressSnapshot(StringUtils.hasText(request.getDeliveryAddress()) ? request.getDeliveryAddress()
                : customer == null ? null : customer.getAddress());
        if ("delivery".equals(request.getFulfillmentType()) && (!StringUtils.hasText(header.getContactNameSnapshot())
                || !StringUtils.hasText(header.getContactPhoneSnapshot()) || !StringUtils.hasText(header.getDeliveryAddressSnapshot())))
            throw new BusinessException(400, "delivery-contact-required");
        header.setHandlerName(request.getHandlerName()); header.setPlannedDeliveryTime(request.getPlannedDeliveryTime());
        header.setDeliveryRemark("delivery".equals(request.getFulfillmentType()) ? request.getDeliveryRemark() : null);
        header.setRecordedTime(header.getRecordedTime() == null ? LocalDateTime.now() : header.getRecordedTime());
        header.setRemark(request.getRemark());
        header.setTotalAmount(BusinessNumbers.money(request.getItems().stream()
                .map(item -> BusinessNumbers.amount(item.getQty(), item.getUnitPrice())).reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    private void createItems(BizSaleOut header, SaleOutCreateRequest request) {
        for (SaleOutItemRequest input : request.getItems()) {
            BigDecimal rate = basicDataService.productUnitRate(input.getProductId(), input.getUnitId());
            BizSaleOutItem item = new BizSaleOutItem();
            item.setSaleOutId(header.getId()); item.setWarehouseId(header.getWarehouseId());
            item.setProductId(input.getProductId()); item.setUnitId(input.getUnitId());
            item.setQty(input.getQty()); item.setBaseQty(BusinessNumbers.baseQuantity(input.getQty(), rate));
            item.setUnitPrice(input.getUnitPrice()); item.setAmount(BusinessNumbers.amount(input.getQty(), input.getUnitPrice()));
            var product = basicDataService.productDetail(input.getProductId());
            item.setProductNameSnapshot(product.getProductName()); item.setSpecSnapshot(product.getSpec());
            item.setUnitNameSnapshot(basicDataService.unitDetail(input.getUnitId()).getUnitName());
            item.setRemark(input.getRemark());
            if (saleOutItemMapper.insert(item) != 1) InventoryLedger.conflict();
        }
    }

    public PageResponse<SaleOutDetailResponse> query(SaleOutQueryRequest request) {
        Page<BizSaleOut> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<BizSaleOut> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(request.getOrderNo()), BizSaleOut::getOrderNo, request.getOrderNo())
                .eq(request.getWarehouseId() != null, BizSaleOut::getWarehouseId, request.getWarehouseId())
                .eq(request.getCustomerId() != null, BizSaleOut::getCustomerId, request.getCustomerId())
                .isNull(Boolean.TRUE.equals(request.getGuest()), BizSaleOut::getCustomerId)
                .like(StringUtils.hasText(request.getCustomerName()), BizSaleOut::getCustomerNameSnapshot, request.getCustomerName())
                .eq(StringUtils.hasText(request.getSaleType()), BizSaleOut::getSaleType, request.getSaleType())
                .eq(StringUtils.hasText(request.getFulfillmentType()), BizSaleOut::getFulfillmentType, request.getFulfillmentType())
                .eq(StringUtils.hasText(request.getOrderStatus()), BizSaleOut::getOrderStatus, request.getOrderStatus())
                .eq(request.getStatus() != null, BizSaleOut::getStatus, request.getStatus())
                .ge(request.getBizDateFrom() != null, BizSaleOut::getBizDate, request.getBizDateFrom())
                .le(request.getBizDateTo() != null, BizSaleOut::getBizDate, request.getBizDateTo())
                .ge(request.getCompletedDateFrom() != null, BizSaleOut::getCompletedTime, request.getCompletedDateFrom() == null ? null : request.getCompletedDateFrom().atStartOfDay())
                .lt(request.getCompletedDateTo() != null, BizSaleOut::getCompletedTime, request.getCompletedDateTo() == null ? null : request.getCompletedDateTo().plusDays(1).atStartOfDay())
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
        response.setEvents(jdbc.query("SELECT event_type,operator_id,detail,created_time FROM sale_order_event WHERE sale_out_id=? ORDER BY id",
                (rs, row) -> {
                    SaleOutDetailResponse.Event event = new SaleOutDetailResponse.Event();
                    event.setEventType(rs.getString("event_type"));
                    long operator = rs.getLong("operator_id"); event.setOperatorId(rs.wasNull() ? null : operator);
                    event.setDetail(rs.getString("detail"));
                    event.setCreatedTime(rs.getTimestamp("created_time").toLocalDateTime());
                    return event;
                }, id));
        return response;
    }

    private SaleOutDetailResponse toResponseWithoutItems(BizSaleOut saleOut) {
        SaleOutDetailResponse response = new SaleOutDetailResponse();
        response.setId(saleOut.getId());
        response.setOrderNo(saleOut.getOrderNo());
        response.setWarehouseId(saleOut.getWarehouseId());
        response.setWarehouseName(basicDataService.warehouseDetail(saleOut.getWarehouseId()).getWarehouseName());
        response.setCustomerId(saleOut.getCustomerId());
        response.setCustomerName(saleOut.getCustomerNameSnapshot());
        response.setSaleType(saleOut.getSaleType()); response.setFulfillmentType(saleOut.getFulfillmentType());
        response.setOrderStatus(saleOut.getOrderStatus()); response.setRecordedTime(saleOut.getRecordedTime());
        response.setCompletedTime(saleOut.getCompletedTime()); response.setContactName(saleOut.getContactNameSnapshot());
        response.setContactPhone(saleOut.getContactPhoneSnapshot()); response.setDeliveryAddress(saleOut.getDeliveryAddressSnapshot());
        response.setDeliveryRemark(saleOut.getDeliveryRemark());
        response.setHandlerName(saleOut.getHandlerName()); response.setPlannedDeliveryTime(saleOut.getPlannedDeliveryTime());
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
        response.setProductName(item.getProductNameSnapshot());
        response.setSpec(item.getSpecSnapshot());
        response.setUnitName(item.getUnitNameSnapshot());
        response.setUnitPrice(item.getUnitPrice());
        response.setAmount(item.getAmount());
        response.setRemark(item.getRemark());
        return response;
    }


}
