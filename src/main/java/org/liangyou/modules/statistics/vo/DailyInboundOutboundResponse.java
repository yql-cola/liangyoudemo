package org.liangyou.modules.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "日进出货统计响应")
public class DailyInboundOutboundResponse {

    @Schema(description = "统计类型", example = "purchase")
    private String statType;
    @Schema(description = "统计日期", example = "2026-04-16")
    private LocalDate statDate;
    @Schema(description = "总数量", example = "100")
    private BigDecimal totalQty;
    @Schema(description = "总金额", example = "12050.00")
    private BigDecimal totalAmount;
}
