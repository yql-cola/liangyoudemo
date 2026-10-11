package org.liangyou.modules.basic.dto;

import lombok.Data;

@Data
public class BasicQueryRequest {

    private int pageNum = 1;
    private int pageSize = 10;
    private String keyword;
    private Integer status;
}
