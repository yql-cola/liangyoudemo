package org.liangyou.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inv_sale_batch_deduction")
public class InvSaleBatchDeduction {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long saleOutId;
    private Long saleOutItemId;
    private Long batchId;
    private BigDecimal deductQty;
    private BigDecimal unitCost;
    private BigDecimal amount;
    private Long createdBy;
    private LocalDateTime createdTime;
}
