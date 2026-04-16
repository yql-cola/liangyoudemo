package org.liangyou.modules.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_purchase_in_item")
public class BizPurchaseInItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long purchaseInId;
    private Long warehouseId;
    private Long productId;
    private Long unitId;
    private BigDecimal qty;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String batchNo;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    private LocalDateTime inboundTime;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdTime;
}
