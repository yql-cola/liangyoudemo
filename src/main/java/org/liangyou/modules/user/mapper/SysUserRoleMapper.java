package org.liangyou.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.liangyou.modules.user.entity.SysUserRole;

import java.util.List;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    @Select("select id, user_id, role_id, created_by, created_time, deleted from sys_user_role where user_id = #{userId}")
    List<SysUserRole> selectAllByUserId(@Param("userId") Long userId);
}
