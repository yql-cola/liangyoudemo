package org.liangyou.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("inv_batch")
public class InvBatch {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long warehouseId;
    private Long productId;
    private String sourceType;
    private Long sourceId;
    private String batchNo;
    private LocalDateTime inboundTime;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    private BigDecimal unitPrice;
    private BigDecimal initQty;
    private BigDecimal remainQty;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    @TableLogic
    private Integer deleted;
    private Integer version;
}
