package org.liangyou.modules.statistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stat_daily_summary")
public class StatDailySummary {

    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private Long warehouseId;
    private String statType;
    private Long productCategoryId;
    private Long customerId;
    private Long supplierId;
    private BigDecimal totalQty;
    private BigDecimal totalAmount;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
