package org.liangyou.modules.statistics.service;

import lombok.RequiredArgsConstructor;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.statistics.dto.StatisticsQueryRequest;
import org.liangyou.modules.statistics.vo.AmountSummaryResponse;
import org.liangyou.modules.statistics.vo.DailyInboundOutboundResponse;
import org.liangyou.modules.statistics.vo.ScenarioStatisticsResponse;
import org.liangyou.modules.statistics.vo.ScenarioStatisticsResponse.Row;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final ScenarioStatisticsService scenarios;

    public byte[] exportScenario(String scenario, StatisticsQueryRequest request) {
        return scenarios.export(scenario, request);
    }

    public ScenarioStatisticsResponse dashboard(StatisticsQueryRequest request) {
        return scenarios.report("dashboard", request);
    }

    public ScenarioStatisticsResponse trends(StatisticsQueryRequest request) {
        return scenarios.report("trends", request);
    }

    public ScenarioStatisticsResponse saleProducts(StatisticsQueryRequest request) {
        return scenarios.report("sale-products", request);
    }

    public ScenarioStatisticsResponse purchaseProducts(StatisticsQueryRequest request) {
        return scenarios.report("purchase-products", request);
    }

    public ScenarioStatisticsResponse inventoryStructure(StatisticsQueryRequest request) {
        return scenarios.report("inventory-structure", request);
    }

    public ScenarioStatisticsResponse customerRanks(StatisticsQueryRequest request) {
        return scenarios.report("customer-ranks", request);
    }

    public ScenarioStatisticsResponse supplierRanks(StatisticsQueryRequest request) {
        return scenarios.report("supplier-ranks", request);
    }

    public ScenarioStatisticsResponse categoryShares(StatisticsQueryRequest request) {
        return scenarios.report("category-shares", request);
    }

    @Transactional(readOnly = true)
    public List<DailyInboundOutboundResponse> dailyInboundOutbound(StatisticsQueryRequest request) {
        String source = businessSource(request);
        List<DailyInboundOutboundResponse> result = new ArrayList<>();
        for (String type : List.of("purchase", "sale")) {
            if (!"all".equals(source) && !type.equals(source)) continue;
            for (Row row : scenarios.aggregateBusiness(request, type, "date").getRows()) {
                DailyInboundOutboundResponse response = new DailyInboundOutboundResponse();
                response.setStatType(type); response.setStatDate(LocalDate.parse(row.getDimensionId()));
                response.setTotalAmount(row.getAmount()); response.setTotalQty(row.getQty());
                result.add(response);
            }
        }
        result.sort(Comparator.comparing(DailyInboundOutboundResponse::getStatDate)
                .thenComparing(DailyInboundOutboundResponse::getStatType));
        return result;
    }

    @Transactional(readOnly = true)
    public List<AmountSummaryResponse> monthlyInboundOutbound(StatisticsQueryRequest request) {
        StatisticsQueryRequest q = new StatisticsQueryRequest(); BeanUtils.copyProperties(request, q);
        try {
            YearMonth from = q.getMonthFrom() == null ? null : YearMonth.parse(q.getMonthFrom());
            YearMonth to = q.getMonthTo() == null ? null : YearMonth.parse(q.getMonthTo());
            if (from != null && to != null && from.isAfter(to))
                throw new BusinessException(400, "statistics-date-range-invalid");
            if (from != null && (q.getDateFrom() == null || q.getDateFrom().isBefore(from.atDay(1))))
                q.setDateFrom(from.atDay(1));
            if (to != null && (q.getDateTo() == null || q.getDateTo().isAfter(to.atEndOfMonth())))
                q.setDateTo(to.atEndOfMonth());
        } catch (DateTimeException ex) {
            throw new BusinessException(400, "statistics-date-range-invalid");
        }
        return scenarios.aggregateBusiness(q, businessSource(q), "month").getRows().stream()
                .sorted(Comparator.comparing(Row::getDimensionId))
                .map(row -> summary(row.getDimensionId(), row)).toList();
    }

    public List<AmountSummaryResponse> amountByCustomer(StatisticsQueryRequest request) {
        return scenarios.aggregateBusiness(request, "sale", "partner").getRows().stream()
                .map(row -> summary("customer:" + row.getDimensionId(), row)).toList();
    }

    public List<AmountSummaryResponse> amountBySupplier(StatisticsQueryRequest request) {
        return scenarios.aggregateBusiness(request, "purchase", "partner").getRows().stream()
                .map(row -> summary("supplier:" + row.getDimensionId(), row)).toList();
    }

    public List<AmountSummaryResponse> amountByCategory(StatisticsQueryRequest request) {
        List<Row> rows = "inventory".equals(request.getStatType())
                ? scenarios.report("category-shares", request).getRows()
                : scenarios.aggregateBusiness(request, businessSource(request), "category").getRows();
        return rows.stream().map(row -> summary("category:" + row.getDimensionId(), row)).toList();
    }

    private String businessSource(StatisticsQueryRequest q) {
        if (q.getStatType() == null) return "all";
        if (!Set.of("purchase", "sale", "all").contains(q.getStatType()))
            throw new BusinessException(400, "statistics-dimension-invalid");
        return q.getStatType();
    }

    private AmountSummaryResponse summary(String key, Row row) {
        AmountSummaryResponse response = new AmountSummaryResponse();
        response.setKey(key); response.setTotalAmount(row.getAmount()); response.setTotalQty(row.getQty());
        return response;
    }
}
