package org.liangyou.modules.basic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("base_product_category")
public class BaseProductCategory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String categoryName;
    private Long parentId;
    private Integer sortNo;
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
