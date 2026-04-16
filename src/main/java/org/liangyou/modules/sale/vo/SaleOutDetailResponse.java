package org.liangyou.modules.sale.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "销售出库详情响应")
public class SaleOutDetailResponse {

    @Schema(description = "销售单 ID", example = "300")
    private Long id;
    @Schema(description = "出库单号", example = "SO20260416123000001")
    private String orderNo;
    @Schema(description = "仓库 ID", example = "1")
    private Long warehouseId;
    @Schema(description = "客户 ID", example = "20")
    private Long customerId;
    @Schema(description = "业务日期", example = "2026-04-16")
    private LocalDate bizDate;
    @Schema(description = "总金额", example = "3000.00")
    private BigDecimal totalAmount;
    @Schema(description = "状态", example = "1")
    private Integer status;
    @Schema(description = "备注", example = "日常销售")
    private String remark;
    @Schema(description = "明细项")
    private List<Item> items;

    @Data
    @Schema(description = "销售明细响应")
    public static class Item {
        @Schema(description = "明细 ID", example = "1")
        private Long id;
        @Schema(description = "商品 ID", example = "1001")
        private Long productId;
        @Schema(description = "单位 ID", example = "1")
        private Long unitId;
        @Schema(description = "数量", example = "20")
        private BigDecimal qty;
        @Schema(description = "销售单价", example = "150.00")
        private BigDecimal unitPrice;
        @Schema(description = "金额", example = "3000.00")
        private BigDecimal amount;
        @Schema(description = "备注", example = "门店零售")
        private String remark;
    }
}
