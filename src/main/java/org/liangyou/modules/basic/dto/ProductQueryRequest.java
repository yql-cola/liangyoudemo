package org.liangyou.modules.basic.dto;

import lombok.Data;

@Data
public class ProductQueryRequest {

    private int pageNum = 1;
    private int pageSize = 10;
    private String keyword;
    private String productCode;
    private String productName;
    private Long categoryId;
    private Integer status;
}
