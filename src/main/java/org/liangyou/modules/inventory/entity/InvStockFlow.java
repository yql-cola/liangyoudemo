package org.liangyou.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inv_stock_flow")
public class InvStockFlow {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long warehouseId;
    private Long productId;
    private Long batchId;
    private String flowType;
    private String bizType;
    private Long bizId;
    private Long bizItemId;
    private BigDecimal changeQty;
    private BigDecimal afterQty;
    private LocalDateTime bizTime;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdTime;
}
