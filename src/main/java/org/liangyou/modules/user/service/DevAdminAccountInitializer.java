package org.liangyou.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.liangyou.modules.user.entity.SysRole;
import org.liangyou.modules.user.entity.SysUser;
import org.liangyou.modules.user.entity.SysUserRole;
import org.liangyou.modules.user.mapper.SysRoleMapper;
import org.liangyou.modules.user.mapper.SysUserMapper;
import org.liangyou.modules.user.mapper.SysUserRoleMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevAdminAccountInitializer implements ApplicationRunner {

    private static final String SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        SysRole role = ensureSuperAdminRole();
        ensureAdminUserWithRole("admin", "admin", "管理员", role.getId());
        ensureAdminUserWithRole("yaoqianglong", "yaoqianglong", "姚强龙", role.getId());
    }

    private SysRole ensureSuperAdminRole() {
        List<SysRole> roles = sysRoleMapper.selectAllByRoleCode(SUPER_ADMIN_ROLE_CODE);
        if (!roles.isEmpty()) {
            return roles.get(0);
        }

        SysRole role = new SysRole();
        role.setRoleCode(SUPER_ADMIN_ROLE_CODE);
        role.setRoleName("超级管理员");
        role.setStatus(1);
        role.setRemark("开发环境默认管理员角色");
        sysRoleMapper.insert(role);
        return role;
    }

    private void ensureAdminUserWithRole(String username, String password, String realName, Long roleId) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("limit 1"));
        if (user == null) {
            user = new SysUser();
            user.setUsername(username);
            user.setPassword(password);
            user.setRealName(realName);
            user.setStatus(1);
            user.setIsSuperAdmin(1);
            sysUserMapper.insert(user);
        }
        ensureUserRoleBinding(user.getId(), roleId);
    }

    private void ensureUserRoleBinding(Long userId, Long roleId) {
        boolean exists = sysUserRoleMapper.selectAllByUserId(userId).stream()
                .anyMatch(relation -> relation.getRoleId() != null && relation.getRoleId().equals(roleId)
                        && (relation.getDeleted() == null || relation.getDeleted() == 0));
        if (exists) {
            return;
        }

        SysUserRole relation = new SysUserRole();
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        sysUserRoleMapper.insert(relation);
    }
}
