package org.liangyou.modules.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("base_product")
public class BaseProduct {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String productCode;
    private String productName;
    private Long categoryId;
    private String brandName;
    private String spec;
    private String barcode;
    private String imageUrl;
    private Long baseUnitId;
    private Integer shelfLifeDays;
    private Integer status;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    @TableLogic
    private Integer deleted;
    private Integer version;
}
