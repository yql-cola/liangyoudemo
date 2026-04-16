package org.liangyou.modules.user.vo;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "用户详情响应")
public class UserDetailResponse {

    @Schema(description = "用户 ID", example = "100")
    private Long id;
    @Schema(description = "用户名", example = "warehouse01")
    private String username;
    @Schema(description = "真实姓名", example = "仓库员工A")
    private String realName;
    @Schema(description = "手机号", example = "13800000000")
    private String phone;
    @Schema(description = "邮箱", example = "warehouse01@test.com")
    private String email;
    @Schema(description = "仓库 ID", example = "1")
    private Long warehouseId;
    @Schema(description = "状态", example = "1")
    private Integer status;
    @Schema(description = "是否超级管理员", example = "0")
    private Integer isSuperAdmin;
    @ArraySchema(schema = @Schema(description = "角色 ID", example = "1"))
    private List<Long> roleIds;
}
