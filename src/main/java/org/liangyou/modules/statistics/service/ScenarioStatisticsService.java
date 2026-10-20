package org.liangyou.modules.statistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.basic.entity.BaseProductCategory;
import org.liangyou.modules.basic.mapper.BaseProductCategoryMapper;
import org.liangyou.modules.basic.service.ExcelExportService;
import org.liangyou.modules.statistics.dto.StatisticsQueryRequest;
import org.liangyou.modules.statistics.mapper.ScenarioStatisticsMapper;
import org.liangyou.modules.statistics.vo.ScenarioStatisticsResponse;
import org.liangyou.modules.statistics.vo.ScenarioStatisticsResponse.Metric;
import org.liangyou.modules.statistics.vo.ScenarioStatisticsResponse.Row;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Real-time SQL aggregation over active business documents and current batch valuation. */
@Service
@RequiredArgsConstructor
public class ScenarioStatisticsService {
    private final ScenarioStatisticsMapper mapper;
    private final BaseProductCategoryMapper categories;
    private final ExcelExportService excel;
    private final JdbcTemplate jdbc;

    /** Shared live facts for compatibility APIs, without chart date filling or an implicit date range. */
    @Transactional(readOnly = true)
    public ScenarioStatisticsResponse aggregateBusiness(StatisticsQueryRequest input, String source, String dimension) {
        return summarize(mapper.business(validated(input), source, dimension));
    }

    @Transactional(readOnly = true)
    public ScenarioStatisticsResponse report(String scenario, StatisticsQueryRequest input) {
        StatisticsQueryRequest q = validated(input);
        return switch (scenario) {
            case "dashboard" -> dashboard(q);
            case "trends" -> trends(q);
            case "sale-products" -> business(q, "sale", "product");
            case "purchase-products" -> business(q, "purchase", "product");
            case "customer-ranks" -> business(q, "sale", "partner");
            case "supplier-ranks" -> business(q, "purchase", "partner");
            case "inventory-structure" -> inventory(q, q.getDimension());
            case "category-shares" -> "inventory".equals(q.getStatType()) ? inventory(q, "category")
                    : business(q, "purchase".equals(q.getStatType()) ? "purchase" : "sale", "category");
            default -> throw new BusinessException(404, "statistics-not-found");
        };
    }

    private ScenarioStatisticsResponse business(StatisticsQueryRequest q, String source, String dimension) {
        ScenarioStatisticsResponse result = summarize(mapper.business(q, source, dimension));
        StatisticsQueryRequest time = timeRange(q);
        result.setTrendRows(timeRows(time, source));
        return result;
    }

    private ScenarioStatisticsResponse trends(StatisticsQueryRequest q) {
        StatisticsQueryRequest time = timeRange(q);
        String source = Set.of("sale", "purchase").contains(Objects.toString(q.getStatType(), "")) ? q.getStatType() : "all";
        ScenarioStatisticsResponse result = summarize(timeRows(time, source));
        result.setTrendRows(result.getRows());
        result.setMetrics(new ArrayList<>(List.of(
                metric("purchaseAmount", "采购金额", sum(result.getRows(), Row::getPurchaseAmount)),
                metric("saleAmount", "销售金额", sum(result.getRows(), Row::getSaleAmount)),
                metric("purchaseQty", "采购基础数量", sum(result.getRows(), Row::getPurchaseQty)),
                metric("saleQty", "销售基础数量", sum(result.getRows(), Row::getSaleQty)))));
        return result;
    }

    private ScenarioStatisticsResponse inventory(StatisticsQueryRequest q, String dimension) {
        ScenarioStatisticsResponse result = summarize(mapper.inventory(q, dimension));
        result.getMetrics().set(0, metric("amount", "库存价值预估", result.getTotalAmount()));
        result.getMetrics().set(1, metric("qty", "库存基础数量", result.getTotalQty()));
        result.getMetrics().add(metric("availableQty", "可用基础数量", sum(result.getRows(), Row::getAvailableQty)));
        result.getMetrics().add(metric("lockedQty", "锁定基础数量", sum(result.getRows(), Row::getLockedQty)));
        return result;
    }

    private ScenarioStatisticsResponse dashboard(StatisticsQueryRequest q) {
        StatisticsQueryRequest time = timeRange(q);
        ScenarioStatisticsResponse result = summarize(mapper.business(time, "sale", "product"));
        result.setSaleTop(top(result.getRows()));
        result.setCustomerTop(top(summarize(mapper.business(time, "sale", "partner")).getRows()));
        result.setSupplierTop(top(summarize(mapper.business(time, "purchase", "partner")).getRows()));
        result.setTrendRows(timeRows(time, "all"));
        StatisticsQueryRequest today = copy(q);
        today.setDateFrom(LocalDate.now()); today.setDateTo(LocalDate.now());
        List<Row> purchases = mapper.business(today, "purchase", "product");
        List<Row> sales = mapper.business(today, "sale", "product");
        List<Row> stock = mapper.inventory(q, "product");
        result.setMetrics(new ArrayList<>(List.of(
                metric("todayPurchaseAmount", "今日采购金额", sum(purchases, Row::getAmount)),
                metric("todaySaleAmount", "今日销售金额", sum(sales, Row::getAmount)),
                metric("todaySaleQty", "今日销售基础数量", sum(sales, Row::getQty)),
                metric("inventoryProductCount", "库存商品数", BigDecimal.valueOf(stock.stream()
                        .filter(r -> zero(r.getQty()).signum() > 0).map(Row::getDimensionCode).distinct().count())),
                metric("inventoryAmount", "库存价值预估", sum(stock, Row::getAmount)))));
        var pending = pendingSales(q);
        result.getMetrics().add(metric("pendingSaleAmount", "待完成订单金额", pending[0]));
        result.getMetrics().add(metric("pendingSaleQty", "待完成基础数量", pending[1]));
        return result;
    }

    private BigDecimal[] pendingSales(StatisticsQueryRequest q) {
        StringBuilder sql = new StringBuilder("""
                SELECT COALESCE(SUM(i.amount),0),COALESCE(SUM(COALESCE(i.base_qty,i.qty)),0)
                FROM biz_sale_out h JOIN biz_sale_out_item i ON i.sale_out_id=h.id
                WHERE h.deleted=0 AND h.order_status IN ('pending_handoff','pending_delivery','in_delivery','awaiting_return')
                """);
        List<Object> args = new ArrayList<>();
        if (q.getWarehouseId() != null) { sql.append(" AND h.warehouse_id=?"); args.add(q.getWarehouseId()); }
        if (q.getCustomerId() != null) { sql.append(" AND h.customer_id=?"); args.add(q.getCustomerId()); }
        if (q.getProductId() != null) { sql.append(" AND i.product_id=?"); args.add(q.getProductId()); }
        if (q.getDateFrom() != null) { sql.append(" AND h.biz_date>=?"); args.add(q.getDateFrom()); }
        if (q.getDateTo() != null) { sql.append(" AND h.biz_date<=?"); args.add(q.getDateTo()); }
        return jdbc.queryForObject(sql.toString(), (rs, row) -> new BigDecimal[]{rs.getBigDecimal(1), rs.getBigDecimal(2)}, args.toArray());
    }

    private List<Row> top(List<Row> rows) { return List.copyOf(rows.subList(0, Math.min(5, rows.size()))); }

    private ScenarioStatisticsResponse summarize(List<Row> rows) {
        ScenarioStatisticsResponse result = new ScenarioStatisticsResponse();
        result.setRows(new ArrayList<>(rows));
        result.setTotalAmount(sum(rows, Row::getAmount));
        result.setTotalQty(sum(rows, Row::getQty));
        int rank = 0;
        for (Row row : rows) {
            row.setAmount(zero(row.getAmount())); row.setQty(zero(row.getQty()));
            row.setRankNo(++rank);
            row.setAmountRate(rate(row.getAmount(), result.getTotalAmount()));
            row.setQtyRate(rate(row.getQty(), result.getTotalQty()));
            row.setAveragePrice(row.getQty().signum() == 0 ? BigDecimal.ZERO
                    : row.getAmount().divide(row.getQty(), 4, RoundingMode.HALF_UP));
        }
        result.setMetrics(new ArrayList<>(List.of(metric("amount", "金额合计", result.getTotalAmount()),
                metric("qty", "基础数量合计", result.getTotalQty()))));
        return result;
    }

    private List<Row> timeRows(StatisticsQueryRequest q, String source) {
        boolean monthly = "month".equals(q.getGroupBy());
        List<Row> data = mapper.business(q, source, monthly ? "month" : "date");
        Map<String, Row> byDate = new HashMap<>();
        for (Row row : data) byDate.put(row.getDimensionId(), row);
        List<Row> filled = new ArrayList<>();
        LocalDate from = monthly ? q.getDateFrom().withDayOfMonth(1) : q.getDateFrom();
        LocalDate to = q.getDateTo();
        for (LocalDate date = from; !date.isAfter(to); date = monthly ? date.plusMonths(1) : date.plusDays(1)) {
            String key = monthly ? YearMonth.from(date).toString() : date.toString();
            Row row = byDate.get(key);
            if (row == null) {
                row = new Row(); row.setDimensionId(key); row.setDimensionCode(key); row.setDimensionName(key);
                row.setAmount(BigDecimal.ZERO); row.setQty(BigDecimal.ZERO);
                row.setPurchaseAmount(BigDecimal.ZERO); row.setSaleAmount(BigDecimal.ZERO);
                row.setPurchaseQty(BigDecimal.ZERO); row.setSaleQty(BigDecimal.ZERO);
            }
            filled.add(row);
        }
        return filled;
    }

    private StatisticsQueryRequest timeRange(StatisticsQueryRequest q) {
        StatisticsQueryRequest time = copy(q);
        if (time.getDateTo() == null) time.setDateTo(LocalDate.now());
        if (time.getDateFrom() == null) time.setDateFrom(time.getDateTo().minusDays(29));
        if (time.getDateFrom().isAfter(time.getDateTo()) || ChronoUnit.DAYS.between(time.getDateFrom(), time.getDateTo()) > 3660)
            throw new BusinessException(400, "statistics-date-range-invalid");
        return time;
    }

    private StatisticsQueryRequest validated(StatisticsQueryRequest input) {
        StatisticsQueryRequest q = copy(input);
        if (q.getDateFrom() != null && q.getDateTo() != null && q.getDateFrom().isAfter(q.getDateTo()))
            throw new BusinessException(400, "statistics-date-range-invalid");
        if (!Set.of("day", "month").contains(Objects.toString(q.getGroupBy(), ""))
                || !Set.of("product", "category", "warehouse").contains(Objects.toString(q.getDimension(), ""))
                || q.getStatType() != null && !Set.of("sale", "purchase", "inventory", "all").contains(q.getStatType()))
            throw new BusinessException(400, "statistics-dimension-invalid");
        for (Long id : Arrays.asList(q.getProductId(), q.getCategoryId(), q.getWarehouseId(), q.getCustomerId(), q.getSupplierId()))
            if (id != null && id <= 0) throw new BusinessException(400, "statistics-filter-invalid");
        q.setCategoryIds(null); // Client cannot inject an internal category scope.
        if (q.getCategoryId() != null) {
            List<BaseProductCategory> all = categories.selectList(new LambdaQueryWrapper<>());
            Set<Long> ids = new LinkedHashSet<>(); ids.add(q.getCategoryId());
            boolean changed;
            do {
                changed = false;
                for (BaseProductCategory category : all)
                    if (ids.contains(category.getParentId())) changed |= ids.add(category.getId());
            } while (changed);
            q.setCategoryIds(new ArrayList<>(ids));
        }
        return q;
    }

    @Transactional(readOnly = true)
    public byte[] export(String scenario, StatisticsQueryRequest q) {
        ScenarioStatisticsResponse report = report(scenario, q);
        List<Map<String, Object>> data = new ArrayList<>();
        for (Metric metric : report.getMetrics()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("数据区", "指标"); row.put("名称", metric.getName()); row.put("指标值", metric.getValue()); data.add(row);
        }
        appendRows(data, "明细", report.getRows());
        if (!"trends".equals(scenario)) appendRows(data, "趋势", report.getTrendRows());
        if ("dashboard".equals(scenario)) {
            appendRows(data, "销售商品前五", report.getSaleTop());
            appendRows(data, "客户前五", report.getCustomerTop()); appendRows(data, "供应商前五", report.getSupplierTop());
        }
        return excel.export(scenario, data);
    }

    private void appendRows(List<Map<String, Object>> data, String section, List<Row> rows) {
        for (Row item : rows) {
            Map<String, Object> row = new LinkedHashMap<>(); row.put("数据区", section);
            row.put("名称", item.getDimensionName()); row.put("编码", item.getDimensionCode());
            row.put("分类", item.getCategoryName()); row.put("仓库", item.getWarehouseName()); row.put("基础单位", item.getUnitName());
            row.put("供应商", item.getSupplierName()); row.put("供应商编码", item.getSupplierCode());
            row.put("金额", item.getAmount()); row.put("基础数量", item.getQty()); row.put("金额占比%", item.getAmountRate());
            row.put("数量占比%", item.getQtyRate()); row.put("排名", item.getRankNo()); row.put("下单次数", item.getOrderCount());
            row.put("基础单位均价", item.getAveragePrice()); row.put("可用基础数量", item.getAvailableQty()); row.put("锁定基础数量", item.getLockedQty());
            row.put("采购金额", item.getPurchaseAmount()); row.put("销售金额", item.getSaleAmount());
            row.put("采购基础数量", item.getPurchaseQty()); row.put("销售基础数量", item.getSaleQty()); data.add(row);
        }
    }

    private static StatisticsQueryRequest copy(StatisticsQueryRequest input) {
        StatisticsQueryRequest q = new StatisticsQueryRequest(); BeanUtils.copyProperties(input, q); return q;
    }
    private static BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private static BigDecimal sum(List<Row> rows, java.util.function.Function<Row, BigDecimal> value) {
        return rows.stream().map(value).map(ScenarioStatisticsService::zero).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    private static BigDecimal rate(BigDecimal part, BigDecimal total) {
        return total.signum() == 0 ? BigDecimal.ZERO : part.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }
    private static Metric metric(String key, String name, BigDecimal value) {
        Metric metric = new Metric(); metric.setKey(key); metric.setName(name); metric.setValue(value); return metric;
    }
}
