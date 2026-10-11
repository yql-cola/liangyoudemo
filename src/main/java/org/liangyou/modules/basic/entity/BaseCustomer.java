package org.liangyou.modules.basic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("base_customer")
public class BaseCustomer {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String customerCode;
    private String customerName;
    private String contactName;
    private String contactPhone;
    private String address;
    private String taxNo;
    private String bankName;
    private String bankAccount;
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
