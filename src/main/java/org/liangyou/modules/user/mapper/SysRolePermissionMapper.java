package org.liangyou.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.liangyou.modules.user.entity.SysRolePermission;

import java.util.List;

@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    @Select("select id, role_id, permission_id, created_by, created_time, deleted from sys_role_permission where role_id = #{roleId}")
    List<SysRolePermission> selectAllByRoleId(@Param("roleId") Long roleId);
}
