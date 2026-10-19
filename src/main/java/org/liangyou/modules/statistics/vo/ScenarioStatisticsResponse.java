package org.liangyou.modules.statistics.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ScenarioStatisticsResponse {

    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal totalQty = BigDecimal.ZERO;
    private List<Metric> metrics = new ArrayList<>();
    private List<Row> rows = new ArrayList<>();
    private List<Row> trendRows = new ArrayList<>();
    private List<Row> saleTop = new ArrayList<>();
    private List<Row> customerTop = new ArrayList<>();
    private List<Row> supplierTop = new ArrayList<>();

    @Data
    public static class Metric {
        private String key;
        private String name;
        private BigDecimal value;
    }

    @Data
    public static class Row {
        private String dimensionId;
        private String dimensionCode;
        private String dimensionName;
        private String categoryName;
        private String spec;
        private String unitName;
        private String warehouseName;
        private String supplierName;
        private String supplierCode;
        private BigDecimal amount;
        private BigDecimal qty;
        private BigDecimal amountRate;
        private BigDecimal qtyRate;
        private Integer rankNo;
        private BigDecimal averagePrice;
        private Long orderCount;
        private BigDecimal availableQty;
        private BigDecimal lockedQty;
        private BigDecimal purchaseAmount;
        private BigDecimal saleAmount;
        private BigDecimal purchaseQty;
        private BigDecimal saleQty;
    }
}
