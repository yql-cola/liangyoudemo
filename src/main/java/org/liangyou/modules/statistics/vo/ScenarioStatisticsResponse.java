package org.liangyou.modules.statistics.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ScenarioStatisticsResponse {

    private List<Metric> metrics = new ArrayList<>();
    private List<Row> rows = new ArrayList<>();

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
        private BigDecimal amount;
        private BigDecimal qty;
        private BigDecimal amountRate;
        private BigDecimal qtyRate;
        private Integer rankNo;
    }
}
