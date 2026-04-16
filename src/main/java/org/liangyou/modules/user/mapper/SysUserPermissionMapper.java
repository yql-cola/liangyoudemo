package org.liangyou.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.liangyou.modules.user.entity.SysUserPermission;

import java.util.List;

@Mapper
public interface SysUserPermissionMapper extends BaseMapper<SysUserPermission> {

    @Select("select id, user_id, permission_id, grant_type, created_by, created_time, deleted from sys_user_permission where user_id = #{userId}")
    List<SysUserPermission> selectAllByUserId(@Param("userId") Long userId);
}
