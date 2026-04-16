package org.liangyou.modules.statistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.liangyou.modules.product.entity.BaseProduct;
import org.liangyou.modules.product.mapper.BaseProductMapper;
import org.liangyou.modules.purchase.entity.BizPurchaseInItem;
import org.liangyou.modules.purchase.entity.BizPurchaseIn;
import org.liangyou.modules.purchase.mapper.BizPurchaseInItemMapper;
import org.liangyou.modules.purchase.mapper.BizPurchaseInMapper;
import org.liangyou.modules.sale.entity.BizSaleOutItem;
import org.liangyou.modules.sale.entity.BizSaleOut;
import org.liangyou.modules.sale.mapper.BizSaleOutItemMapper;
import org.liangyou.modules.sale.mapper.BizSaleOutMapper;
import org.liangyou.modules.statistics.dto.StatisticsQueryRequest;
import org.liangyou.modules.statistics.entity.StatDailySummary;
import org.liangyou.modules.statistics.entity.StatMonthlySummary;
import org.liangyou.modules.statistics.mapper.StatDailySummaryMapper;
import org.liangyou.modules.statistics.mapper.StatMonthlySummaryMapper;
import org.liangyou.modules.statistics.vo.AmountSummaryResponse;
import org.liangyou.modules.statistics.vo.DailyInboundOutboundResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final StatDailySummaryMapper statDailySummaryMapper;
    private final StatMonthlySummaryMapper statMonthlySummaryMapper;
    private final BizPurchaseInMapper purchaseInMapper;
    private final BizSaleOutMapper saleOutMapper;
    private final BizPurchaseInItemMapper purchaseInItemMapper;
    private final BizSaleOutItemMapper saleOutItemMapper;
    private final BaseProductMapper baseProductMapper;

    public List<DailyInboundOutboundResponse> dailyInboundOutbound(StatisticsQueryRequest request) {
        List<StatDailySummary> summaries = statDailySummaryMapper == null ? List.of() : statDailySummaryMapper.selectList(
                new LambdaQueryWrapper<StatDailySummary>()
                        .eq(request.getWarehouseId() != null, StatDailySummary::getWarehouseId, request.getWarehouseId())
                        .eq(request.getStatType() != null, StatDailySummary::getStatType, request.getStatType())
                        .ge(request.getDateFrom() != null, StatDailySummary::getStatDate, request.getDateFrom())
                        .le(request.getDateTo() != null, StatDailySummary::getStatDate, request.getDateTo())
                        .orderByAsc(StatDailySummary::getStatDate));
        if (!summaries.isEmpty()) {
            return summaries.stream().map(this::toDailyResponse).toList();
        }

        List<DailyInboundOutboundResponse> fallback = new ArrayList<>();
        if (purchaseInMapper != null) {
            fallback.addAll(purchaseInMapper.selectList(new LambdaQueryWrapper<BizPurchaseIn>()
                            .eq(request.getWarehouseId() != null, BizPurchaseIn::getWarehouseId, request.getWarehouseId())
                            .ge(request.getDateFrom() != null, BizPurchaseIn::getBizDate, request.getDateFrom())
                            .le(request.getDateTo() != null, BizPurchaseIn::getBizDate, request.getDateTo()))
                    .stream()
                    .map(item -> toDailyResponse("purchase", item.getBizDate(), item.getTotalAmount()))
                    .toList());
        }
        if (saleOutMapper != null) {
            fallback.addAll(saleOutMapper.selectList(new LambdaQueryWrapper<BizSaleOut>()
                            .eq(request.getWarehouseId() != null, BizSaleOut::getWarehouseId, request.getWarehouseId())
                            .ge(request.getDateFrom() != null, BizSaleOut::getBizDate, request.getDateFrom())
                            .le(request.getDateTo() != null, BizSaleOut::getBizDate, request.getDateTo()))
                    .stream()
                    .map(item -> toDailyResponse("sale", item.getBizDate(), item.getTotalAmount()))
                    .toList());
        }
        fallback.sort(Comparator.comparing(DailyInboundOutboundResponse::getStatDate));
        return fallback;
    }

    public List<AmountSummaryResponse> monthlyAmountSummary(StatisticsQueryRequest request) {
        List<StatMonthlySummary> summaries = statMonthlySummaryMapper == null ? List.of() : statMonthlySummaryMapper.selectList(
                new LambdaQueryWrapper<StatMonthlySummary>()
                        .eq(request.getWarehouseId() != null, StatMonthlySummary::getWarehouseId, request.getWarehouseId())
                        .eq(request.getStatType() != null, StatMonthlySummary::getStatType, request.getStatType())
                        .ge(request.getMonthFrom() != null, StatMonthlySummary::getStatMonth, request.getMonthFrom())
                        .le(request.getMonthTo() != null, StatMonthlySummary::getStatMonth, request.getMonthTo())
                        .orderByAsc(StatMonthlySummary::getStatMonth));
        return summaries.stream().map(item -> {
            AmountSummaryResponse response = new AmountSummaryResponse();
            response.setKey(item.getStatMonth());
            response.setTotalAmount(item.getTotalAmount());
            response.setTotalQty(item.getTotalQty());
            return response;
        }).toList();
    }

    public List<AmountSummaryResponse> amountByCustomer(StatisticsQueryRequest request) {
        if (statDailySummaryMapper == null) {
            return List.of();
        }
        return statDailySummaryMapper.selectList(new LambdaQueryWrapper<StatDailySummary>()
                        .isNotNull(StatDailySummary::getCustomerId)
                        .eq(request.getWarehouseId() != null, StatDailySummary::getWarehouseId, request.getWarehouseId())
                        .ge(request.getDateFrom() != null, StatDailySummary::getStatDate, request.getDateFrom())
                        .le(request.getDateTo() != null, StatDailySummary::getStatDate, request.getDateTo()))
                .stream()
                .map(item -> toAmountSummary("customer:" + item.getCustomerId(), item.getTotalAmount(), item.getTotalQty()))
                .toList();
    }

    public List<AmountSummaryResponse> amountBySupplier(StatisticsQueryRequest request) {
        if (statDailySummaryMapper == null) {
            return List.of();
        }
        return statDailySummaryMapper.selectList(new LambdaQueryWrapper<StatDailySummary>()
                        .isNotNull(StatDailySummary::getSupplierId)
                        .eq(request.getWarehouseId() != null, StatDailySummary::getWarehouseId, request.getWarehouseId())
                        .ge(request.getDateFrom() != null, StatDailySummary::getStatDate, request.getDateFrom())
                        .le(request.getDateTo() != null, StatDailySummary::getStatDate, request.getDateTo()))
                .stream()
                .map(item -> toAmountSummary("supplier:" + item.getSupplierId(), item.getTotalAmount(), item.getTotalQty()))
                .toList();
    }

    public List<AmountSummaryResponse> amountByCategory(StatisticsQueryRequest request) {
        if (statDailySummaryMapper == null) {
            return fallbackAmountByCategory(request);
        }
        List<AmountSummaryResponse> summaries = statDailySummaryMapper.selectList(new LambdaQueryWrapper<StatDailySummary>()
                        .isNotNull(StatDailySummary::getProductCategoryId)
                        .eq(request.getWarehouseId() != null, StatDailySummary::getWarehouseId, request.getWarehouseId())
                        .eq(request.getStatType() != null, StatDailySummary::getStatType, request.getStatType())
                        .ge(request.getDateFrom() != null, StatDailySummary::getStatDate, request.getDateFrom())
                        .le(request.getDateTo() != null, StatDailySummary::getStatDate, request.getDateTo()))
                .stream()
                .map(item -> toAmountSummary("category:" + item.getProductCategoryId(), item.getTotalAmount(), item.getTotalQty()))
                .toList();
        if (!summaries.isEmpty()) {
            return summaries;
        }
        return fallbackAmountByCategory(request);
    }

    private DailyInboundOutboundResponse toDailyResponse(StatDailySummary summary) {
        DailyInboundOutboundResponse response = new DailyInboundOutboundResponse();
        response.setStatType(summary.getStatType());
        response.setStatDate(summary.getStatDate());
        response.setTotalQty(summary.getTotalQty());
        response.setTotalAmount(summary.getTotalAmount());
        return response;
    }

    private DailyInboundOutboundResponse toDailyResponse(String statType, LocalDate statDate, BigDecimal totalAmount) {
        DailyInboundOutboundResponse response = new DailyInboundOutboundResponse();
        response.setStatType(statType);
        response.setStatDate(statDate);
        response.setTotalQty(BigDecimal.ZERO);
        response.setTotalAmount(totalAmount);
        return response;
    }

    private AmountSummaryResponse toAmountSummary(String key, BigDecimal totalAmount, BigDecimal totalQty) {
        AmountSummaryResponse response = new AmountSummaryResponse();
        response.setKey(key);
        response.setTotalAmount(totalAmount);
        response.setTotalQty(totalQty);
        return response;
    }

    private List<AmountSummaryResponse> fallbackAmountByCategory(StatisticsQueryRequest request) {
        Map<Long, AmountSummaryResponse> categorySummary = new LinkedHashMap<>();
        if (request.getStatType() == null || "purchase".equals(request.getStatType())) {
            accumulatePurchaseCategorySummary(request, categorySummary);
        }
        if (request.getStatType() == null || "sale".equals(request.getStatType())) {
            accumulateSaleCategorySummary(request, categorySummary);
        }
        return List.copyOf(categorySummary.values());
    }

    private void accumulatePurchaseCategorySummary(StatisticsQueryRequest request,
                                                   Map<Long, AmountSummaryResponse> categorySummary) {
        if (purchaseInMapper == null || purchaseInItemMapper == null || baseProductMapper == null) {
            return;
        }
        List<BizPurchaseIn> orders = purchaseInMapper.selectList(new LambdaQueryWrapper<BizPurchaseIn>()
                .eq(request.getWarehouseId() != null, BizPurchaseIn::getWarehouseId, request.getWarehouseId())
                .ge(request.getDateFrom() != null, BizPurchaseIn::getBizDate, request.getDateFrom())
                .le(request.getDateTo() != null, BizPurchaseIn::getBizDate, request.getDateTo()));
        if (orders.isEmpty()) {
            return;
        }
        Set<Long> orderIds = orders.stream().map(BizPurchaseIn::getId).collect(Collectors.toSet());
        List<BizPurchaseInItem> items = purchaseInItemMapper.selectList(new LambdaQueryWrapper<BizPurchaseInItem>()
                .in(BizPurchaseInItem::getPurchaseInId, orderIds));
        Map<Long, Long> productCategoryMap = loadProductCategoryMap(items.stream()
                .map(BizPurchaseInItem::getProductId)
                .collect(Collectors.toSet()));
        for (BizPurchaseInItem item : items) {
            Long categoryId = productCategoryMap.get(item.getProductId());
            if (categoryId == null) {
                continue;
            }
            mergeCategorySummary(categorySummary, categoryId, item.getAmount(), item.getQty());
        }
    }

    private void accumulateSaleCategorySummary(StatisticsQueryRequest request,
                                               Map<Long, AmountSummaryResponse> categorySummary) {
        if (saleOutMapper == null || saleOutItemMapper == null || baseProductMapper == null) {
            return;
        }
        List<BizSaleOut> orders = saleOutMapper.selectList(new LambdaQueryWrapper<BizSaleOut>()
                .eq(request.getWarehouseId() != null, BizSaleOut::getWarehouseId, request.getWarehouseId())
                .ge(request.getDateFrom() != null, BizSaleOut::getBizDate, request.getDateFrom())
                .le(request.getDateTo() != null, BizSaleOut::getBizDate, request.getDateTo()));
        if (orders.isEmpty()) {
            return;
        }
        Set<Long> orderIds = orders.stream().map(BizSaleOut::getId).collect(Collectors.toSet());
        List<BizSaleOutItem> items = saleOutItemMapper.selectList(new LambdaQueryWrapper<BizSaleOutItem>()
                .in(BizSaleOutItem::getSaleOutId, orderIds));
        Map<Long, Long> productCategoryMap = loadProductCategoryMap(items.stream()
                .map(BizSaleOutItem::getProductId)
                .collect(Collectors.toSet()));
        for (BizSaleOutItem item : items) {
            Long categoryId = productCategoryMap.get(item.getProductId());
            if (categoryId == null) {
                continue;
            }
            mergeCategorySummary(categorySummary, categoryId, item.getAmount(), item.getQty());
        }
    }

    private Map<Long, Long> loadProductCategoryMap(Set<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return baseProductMapper.selectList(new LambdaQueryWrapper<BaseProduct>().in(BaseProduct::getId, productIds))
                .stream()
                .collect(Collectors.toMap(BaseProduct::getId, BaseProduct::getCategoryId));
    }

    private void mergeCategorySummary(Map<Long, AmountSummaryResponse> categorySummary,
                                      Long categoryId,
                                      BigDecimal amount,
                                      BigDecimal qty) {
        AmountSummaryResponse response = categorySummary.computeIfAbsent(categoryId,
                id -> toAmountSummary("category:" + id, BigDecimal.ZERO, BigDecimal.ZERO));
        response.setTotalAmount(response.getTotalAmount().add(amount));
        response.setTotalQty(response.getTotalQty().add(qty));
    }
}
