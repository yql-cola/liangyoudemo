package org.liangyou.modules.statistics.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AmountSummaryResponse {

    private String key;
    private BigDecimal totalAmount;
    private BigDecimal totalQty;
}
