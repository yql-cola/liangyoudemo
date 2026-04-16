package org.liangyou.modules.statistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.liangyou.modules.purchase.entity.BizPurchaseIn;
import org.liangyou.modules.purchase.mapper.BizPurchaseInMapper;
import org.liangyou.modules.sale.entity.BizSaleOut;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final StatDailySummaryMapper statDailySummaryMapper;
    private final StatMonthlySummaryMapper statMonthlySummaryMapper;
    private final BizPurchaseInMapper purchaseInMapper;
    private final BizSaleOutMapper saleOutMapper;

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
}
