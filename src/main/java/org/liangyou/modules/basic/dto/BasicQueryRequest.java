package org.liangyou.modules.basic.dto;

import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Data
public class BasicQueryRequest {

    private int pageNum = 1;
    private int pageSize = 10;

    public int getPageNum() { return Math.max(1, pageNum); }
    public int getPageSize() { return Math.min(100, Math.max(1, pageSize)); }
    private String keyword;
    private String code;
    private String name;
    private Long productId;
    private Long fromUnitId;
    private Long toUnitId;
    @Min(value = 0, message = "status-invalid")
    @Max(value = 1, message = "status-invalid")
    private Integer status;
}
