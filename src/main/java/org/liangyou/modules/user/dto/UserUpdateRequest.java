package org.liangyou.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @NotBlank(message = "real-name-required")
    private String realName;
    private String phone;
    private String email;
    @NotNull(message = "warehouse-id-required")
    private Long warehouseId;
    @NotNull(message = "status-required")
    private Integer status;
    private Integer isSuperAdmin = 0;
}
