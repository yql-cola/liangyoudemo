package org.liangyou.modules.basic.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductResponse {

    private Long id;
    private String productCode;
    private String productName;
    private Long categoryId;
    private String categoryName;
    private String brandName;
    private String spec;
    private String barcode;
    private String imageUrl;
    private Long baseUnitId;
    private String baseUnitName;
    private Integer shelfLifeDays;
    private Integer status;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
