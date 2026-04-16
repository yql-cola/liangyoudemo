package org.liangyou.modules.user.dto;

import lombok.Data;

@Data
public class UserQueryRequest {

    private long pageNum = 1;
    private long pageSize = 10;
    private String username;
    private String realName;
    private Long warehouseId;
    private Integer status;
}
