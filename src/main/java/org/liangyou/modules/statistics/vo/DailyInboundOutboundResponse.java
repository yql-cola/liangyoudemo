package org.liangyou.modules.statistics.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DailyInboundOutboundResponse {

    private String statType;
    private LocalDate statDate;
    private BigDecimal totalQty;
    private BigDecimal totalAmount;
}
