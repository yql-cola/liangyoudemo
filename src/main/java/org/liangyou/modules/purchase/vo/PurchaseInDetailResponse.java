package org.liangyou.modules.purchase.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "采购入库详情响应")
public class PurchaseInDetailResponse {

    @Schema(description = "采购单 ID", example = "200")
    private Long id;
    @Schema(description = "入库单号", example = "PI20260416123000001")
    private String orderNo;
    @Schema(description = "仓库 ID", example = "1")
    private Long warehouseId;
    @Schema(description = "供应商 ID", example = "10")
    private Long supplierId;
    @Schema(description = "业务日期", example = "2026-04-16")
    private LocalDate bizDate;
    @Schema(description = "总金额", example = "12050.00")
    private BigDecimal totalAmount;
    @Schema(description = "状态", example = "1")
    private Integer status;
    @Schema(description = "备注", example = "首批采购")
    private String remark;
    @Schema(description = "明细项")
    private List<Item> items;

    @Data
    @Schema(description = "采购明细响应")
    public static class Item {
        @Schema(description = "明细 ID", example = "1")
        private Long id;
        @Schema(description = "商品 ID", example = "1001")
        private Long productId;
        @Schema(description = "单位 ID", example = "1")
        private Long unitId;
        @Schema(description = "数量", example = "100")
        private BigDecimal qty;
        @Schema(description = "单价", example = "120.50")
        private BigDecimal unitPrice;
        @Schema(description = "金额", example = "12050.00")
        private BigDecimal amount;
        @Schema(description = "批次号", example = "B20260416001")
        private String batchNo;
        @Schema(description = "生产日期", example = "2026-04-01")
        private LocalDate productionDate;
        @Schema(description = "过期日期", example = "2026-10-01")
        private LocalDate expiryDate;
        @Schema(description = "入库时间", example = "2026-04-16T10:00:00")
        private LocalDateTime inboundTime;
        @Schema(description = "备注", example = "整箱入库")
        private String remark;
    }
}
