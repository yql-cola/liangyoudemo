package org.liangyou.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户分页查询参数")
public class UserQueryRequest {

    @Schema(description = "页码", example = "1")
    private long pageNum = 1;
    @Schema(description = "每页条数", example = "10")
    private long pageSize = 10;
    @Schema(description = "用户名", example = "warehouse")
    private String username;
    @Schema(description = "真实姓名", example = "仓库")
    private String realName;
    @Schema(description = "仓库 ID", example = "1")
    private Long warehouseId;
    @Schema(description = "状态", example = "1")
    private Integer status;
}
