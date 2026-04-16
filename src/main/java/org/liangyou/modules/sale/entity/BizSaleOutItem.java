package org.liangyou.modules.sale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_sale_out_item")
public class BizSaleOutItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long saleOutId;
    private Long warehouseId;
    private Long productId;
    private Long unitId;
    private BigDecimal qty;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdTime;
}
