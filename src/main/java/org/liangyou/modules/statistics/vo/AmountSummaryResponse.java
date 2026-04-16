package org.liangyou.modules.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "金额统计响应")
public class AmountSummaryResponse {

    @Schema(description = "聚合维度键值", example = "supplier-10")
    private String key;
    @Schema(description = "总金额", example = "56000.00")
    private BigDecimal totalAmount;
    @Schema(description = "总数量", example = "520")
    private BigDecimal totalQty;
}
