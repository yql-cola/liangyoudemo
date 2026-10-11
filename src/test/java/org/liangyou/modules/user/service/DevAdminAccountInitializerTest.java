package org.liangyou.modules.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.liangyou.modules.user.entity.SysRole;
import org.liangyou.modules.user.entity.SysUser;
import org.liangyou.modules.user.entity.SysUserRole;
import org.liangyou.modules.user.mapper.SysRoleMapper;
import org.liangyou.modules.user.mapper.SysUserMapper;
import org.liangyou.modules.user.mapper.SysUserRoleMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevAdminAccountInitializerTest {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    @Test
    void runCreatesMissingRoleUsersAndBindings() throws Exception {
        when(sysRoleMapper.selectAllByRoleCode("SUPER_ADMIN")).thenReturn(Collections.emptyList());
        when(sysUserMapper.selectOne(any())).thenReturn(null);
        when(sysUserRoleMapper.selectAllByUserId(any())).thenReturn(Collections.emptyList());

        DevAdminAccountInitializer initializer = new DevAdminAccountInitializer(
                sysUserMapper, sysRoleMapper, sysUserRoleMapper);

        initializer.run(null);

        ArgumentCaptor<SysRole> roleCaptor = ArgumentCaptor.forClass(SysRole.class);
        verify(sysRoleMapper).insert(roleCaptor.capture());
        assertEquals("SUPER_ADMIN", roleCaptor.getValue().getRoleCode());
        assertEquals("超级管理员", roleCaptor.getValue().getRoleName());

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper, times(2)).insert(userCaptor.capture());
        List<SysUser> insertedUsers = userCaptor.getAllValues();
        assertEquals(List.of("admin", "yaoqianglong"),
                insertedUsers.stream().map(SysUser::getUsername).toList());
        assertEquals(List.of(1, 1),
                insertedUsers.stream().map(SysUser::getIsSuperAdmin).toList());
        assertEquals(List.of(1, 1),
                insertedUsers.stream().map(SysUser::getStatus).toList());

        verify(sysUserRoleMapper, times(2)).insert(any(SysUserRole.class));
    }

    @Test
    void runSkipsExistingUsersRoleAndBindings() throws Exception {
        SysRole existingRole = new SysRole();
        existingRole.setId(8L);
        existingRole.setRoleCode("SUPER_ADMIN");
        existingRole.setRoleName("超级管理员");
        existingRole.setStatus(1);

        SysUser admin = new SysUser();
        admin.setId(11L);
        admin.setUsername("admin");
        admin.setStatus(1);
        admin.setIsSuperAdmin(1);

        SysUser yaoqianglong = new SysUser();
        yaoqianglong.setId(12L);
        yaoqianglong.setUsername("yaoqianglong");
        yaoqianglong.setStatus(1);
        yaoqianglong.setIsSuperAdmin(1);

        when(sysRoleMapper.selectAllByRoleCode("SUPER_ADMIN")).thenReturn(List.of(existingRole));
        when(sysUserMapper.selectOne(any())).thenReturn(admin, yaoqianglong);

        SysUserRole adminBinding = new SysUserRole();
        adminBinding.setUserId(11L);
        adminBinding.setRoleId(8L);
        SysUserRole yaoqianglongBinding = new SysUserRole();
        yaoqianglongBinding.setUserId(12L);
        yaoqianglongBinding.setRoleId(8L);
        when(sysUserRoleMapper.selectAllByUserId(11L)).thenReturn(List.of(adminBinding));
        when(sysUserRoleMapper.selectAllByUserId(12L)).thenReturn(List.of(yaoqianglongBinding));

        DevAdminAccountInitializer initializer = new DevAdminAccountInitializer(
                sysUserMapper, sysRoleMapper, sysUserRoleMapper);

        initializer.run(null);

        verify(sysRoleMapper, never()).insert(any(SysRole.class));
        verify(sysUserMapper, never()).insert(any(SysUser.class));
        verify(sysUserRoleMapper, never()).insert(any(SysUserRole.class));
    }
}
