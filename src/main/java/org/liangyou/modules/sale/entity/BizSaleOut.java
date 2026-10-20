package org.liangyou.modules.sale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_sale_out")
public class BizSaleOut {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long warehouseId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long customerId;
    private LocalDate bizDate;
    private String saleType;
    private String fulfillmentType;
    private String orderStatus;
    private String customerNameSnapshot;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String contactNameSnapshot;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String contactPhoneSnapshot;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String deliveryAddressSnapshot;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String deliveryRemark;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String handlerName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime plannedDeliveryTime;
    private LocalDateTime completedTime;
    private LocalDateTime recordedTime;
    private BigDecimal totalAmount;
    private Integer status;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    @TableLogic
    private Integer deleted;
    private Integer version;
}
