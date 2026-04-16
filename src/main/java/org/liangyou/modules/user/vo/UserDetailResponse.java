package org.liangyou.modules.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserDetailResponse {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Long warehouseId;
    private Integer status;
    private Integer isSuperAdmin;
    private List<Long> roleIds;
}
