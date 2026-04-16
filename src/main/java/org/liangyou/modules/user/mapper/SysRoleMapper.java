package org.liangyou.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;
import org.liangyou.modules.user.entity.SysRole;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("select id, role_code, role_name, status, remark, created_by, created_time, updated_by, updated_time, deleted, version from sys_role where role_code = #{roleCode}")
    List<SysRole> selectAllByRoleCode(@Param("roleCode") String roleCode);
}
