package org.liangyou.modules.basic.vo;

import lombok.Data;

@Data
public class BasicOptionResponse {

    private Long id;
    private String code;
    private String name;
    private Integer status;
    private Long categoryId;
    private String categoryName;
    private Long baseUnitId;
    private String baseUnitName;
    private String spec;
    private String brandName;
    private Long productId;
    private String productName;
    private Long fromUnitId;
    private String fromUnitName;
    private Long toUnitId;
    private String toUnitName;
    private java.math.BigDecimal convertRate;
}
