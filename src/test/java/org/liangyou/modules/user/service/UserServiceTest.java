package org.liangyou.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.user.dto.RoleCreateRequest;
import org.liangyou.modules.user.dto.RolePermissionAssignRequest;
import org.liangyou.modules.user.dto.RoleUpdateRequest;
import org.liangyou.modules.user.entity.SysPermission;
import org.liangyou.modules.user.entity.SysRole;
import org.liangyou.modules.user.entity.SysRolePermission;
import org.liangyou.modules.user.entity.SysUser;
import org.liangyou.modules.user.entity.SysUserPermission;
import org.liangyou.modules.user.entity.SysUserRole;
import org.liangyou.modules.user.mapper.SysPermissionMapper;
import org.liangyou.modules.user.mapper.SysRoleMapper;
import org.liangyou.modules.user.mapper.SysRolePermissionMapper;
import org.liangyou.modules.user.mapper.SysUserMapper;
import org.liangyou.modules.user.mapper.SysUserPermissionMapper;
import org.liangyou.modules.user.mapper.SysUserRoleMapper;
import org.liangyou.modules.user.vo.UserPermissionResponse;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysUserRoleMapper sysUserRoleMapper;
    @Mock
    private SysRolePermissionMapper sysRolePermissionMapper;
    @Mock
    private SysUserPermissionMapper sysUserPermissionMapper;
    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private SysPermissionMapper sysPermissionMapper;

    @Test
    void createRoleRejectsRoleCodeUsedBySoftDeletedRow() {
        UserService userService = new UserService(sysUserMapper, sysUserRoleMapper, sysRolePermissionMapper,
                sysUserPermissionMapper, sysRoleMapper, sysPermissionMapper);

        SysRole deletedRole = new SysRole();
        deletedRole.setId(99L);
        deletedRole.setRoleCode("WAREHOUSE");
        deletedRole.setDeleted(1);
        when(sysRoleMapper.selectAllByRoleCode("WAREHOUSE")).thenReturn(List.of(deletedRole));

        RoleCreateRequest request = new RoleCreateRequest();
        request.setRoleCode("WAREHOUSE");
        request.setRoleName("仓库人员");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.createRole(request));
        assertEquals(400, exception.getCode());
        assertEquals("role-code-exists", exception.getMessage());
        verify(sysRoleMapper, never()).insert(any(SysRole.class));
    }

    @Test
    void updateRoleRejectsRoleCodeUsedBySoftDeletedRow() {
        UserService userService = new UserService(sysUserMapper, sysUserRoleMapper, sysRolePermissionMapper,
                sysUserPermissionMapper, sysRoleMapper, sysPermissionMapper);

        SysRole currentRole = new SysRole();
        currentRole.setId(1L);
        currentRole.setRoleCode("FINANCE");
        currentRole.setStatus(1);
        when(sysRoleMapper.selectById(1L)).thenReturn(currentRole);

        SysRole deletedRole = new SysRole();
        deletedRole.setId(99L);
        deletedRole.setRoleCode("WAREHOUSE");
        deletedRole.setDeleted(1);
        when(sysRoleMapper.selectAllByRoleCode("WAREHOUSE")).thenReturn(List.of(deletedRole));

        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setRoleCode("WAREHOUSE");
        request.setRoleName("仓库人员");
        request.setStatus(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.updateRole(1L, request));
        assertEquals(400, exception.getCode());
        assertEquals("role-code-exists", exception.getMessage());
        verify(sysRoleMapper, never()).updateById(any(SysRole.class));
    }

    @Test
    void assignRolePermissionsRejectsMissingPermissionIds() {
        UserService userService = new UserService(sysUserMapper, sysUserRoleMapper, sysRolePermissionMapper,
                sysUserPermissionMapper, sysRoleMapper, sysPermissionMapper);

        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleCode("WAREHOUSE");
        role.setRoleName("仓库人员");
        role.setStatus(1);
        when(sysRoleMapper.selectById(1L)).thenReturn(role);

        SysPermission existing = new SysPermission();
        existing.setId(101L);
        existing.setStatus(1);
        when(sysPermissionMapper.selectBatchIds(List.of(101L, 102L))).thenReturn(List.of(existing));

        RolePermissionAssignRequest request = new RolePermissionAssignRequest();
        request.setPermissionIds(List.of(101L, 102L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.assignRolePermissions(1L, request));
        assertEquals(404, exception.getCode());
        assertEquals("permission-not-found", exception.getMessage());
        verify(sysRolePermissionMapper, never()).delete(any(LambdaQueryWrapper.class));
        verify(sysRolePermissionMapper, never()).insert(any(SysRolePermission.class));
    }

    @Test
    void assignRolePermissionsRejectsDisabledPermissionIds() {
        UserService userService = new UserService(sysUserMapper, sysUserRoleMapper, sysRolePermissionMapper,
                sysUserPermissionMapper, sysRoleMapper, sysPermissionMapper);

        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleCode("WAREHOUSE");
        role.setRoleName("仓库人员");
        role.setStatus(1);
        when(sysRoleMapper.selectById(1L)).thenReturn(role);

        SysPermission disabled = new SysPermission();
        disabled.setId(101L);
        disabled.setStatus(0);
        when(sysPermissionMapper.selectBatchIds(List.of(101L))).thenReturn(List.of(disabled));

        RolePermissionAssignRequest request = new RolePermissionAssignRequest();
        request.setPermissionIds(List.of(101L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.assignRolePermissions(1L, request));
        assertEquals(400, exception.getCode());
        assertEquals("permission-disabled", exception.getMessage());
        verify(sysRolePermissionMapper, never()).delete(any(LambdaQueryWrapper.class));
        verify(sysRolePermissionMapper, never()).insert(any(SysRolePermission.class));
    }

    @Test
    void assignRolePermissionsReactivatesDeletedRelationInsteadOfInsertingDuplicate() {
        UserService userService = new UserService(sysUserMapper, sysUserRoleMapper, sysRolePermissionMapper,
                sysUserPermissionMapper, sysRoleMapper, sysPermissionMapper);

        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleCode("WAREHOUSE");
        role.setRoleName("仓库人员");
        role.setStatus(1);
        when(sysRoleMapper.selectById(1L)).thenReturn(role);

        SysPermission permission = new SysPermission();
        permission.setId(101L);
        permission.setStatus(1);
        when(sysPermissionMapper.selectBatchIds(List.of(101L))).thenReturn(List.of(permission));

        SysRolePermission deletedRelation = new SysRolePermission();
        deletedRelation.setId(10L);
        deletedRelation.setRoleId(1L);
        deletedRelation.setPermissionId(101L);
        deletedRelation.setDeleted(1);
        when(sysRolePermissionMapper.selectAllByRoleId(1L)).thenReturn(List.of(deletedRelation));

        RolePermissionAssignRequest request = new RolePermissionAssignRequest();
        request.setPermissionIds(List.of(101L));

        userService.assignRolePermissions(1L, request);

        verify(sysRolePermissionMapper, never()).delete(any(LambdaQueryWrapper.class));
        verify(sysRolePermissionMapper, times(1)).updateById(any(SysRolePermission.class));
        verify(sysRolePermissionMapper, never()).insert(any(SysRolePermission.class));
    }

    @Test
    void assignRolesRejectsDisabledRoleIds() {
        UserService userService = new UserService(sysUserMapper, sysUserRoleMapper, sysRolePermissionMapper,
                sysUserPermissionMapper, sysRoleMapper, sysPermissionMapper);

        SysUser user = new SysUser();
        user.setId(100L);
        when(sysUserMapper.selectById(100L)).thenReturn(user);

        SysRole activeRole = new SysRole();
        activeRole.setId(1L);
        activeRole.setStatus(1);
        SysRole disabledRole = new SysRole();
        disabledRole.setId(2L);
        disabledRole.setStatus(0);
        when(sysRoleMapper.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(activeRole, disabledRole));

        org.liangyou.modules.user.dto.UserRoleAssignRequest request = new org.liangyou.modules.user.dto.UserRoleAssignRequest();
        request.setRoleIds(List.of(1L, 2L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.assignRoles(100L, request));
        assertEquals(400, exception.getCode());
        assertEquals("role-disabled", exception.getMessage());
        verify(sysUserRoleMapper, never()).updateById(any(SysUserRole.class));
        verify(sysUserRoleMapper, never()).insert(any(SysUserRole.class));
    }

    @Test
    void assignRolesRejectsMissingRoleIds() {
        UserService userService = new UserService(sysUserMapper, sysUserRoleMapper, sysRolePermissionMapper,
                sysUserPermissionMapper, sysRoleMapper, sysPermissionMapper);

        SysUser user = new SysUser();
        user.setId(100L);
        when(sysUserMapper.selectById(100L)).thenReturn(user);

        SysRole activeRole = new SysRole();
        activeRole.setId(1L);
        activeRole.setStatus(1);
        when(sysRoleMapper.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(activeRole));

        org.liangyou.modules.user.dto.UserRoleAssignRequest request = new org.liangyou.modules.user.dto.UserRoleAssignRequest();
        request.setRoleIds(List.of(1L, 2L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.assignRoles(100L, request));
        assertEquals(404, exception.getCode());
        assertEquals("role-not-found", exception.getMessage());
        verify(sysUserRoleMapper, never()).updateById(any(SysUserRole.class));
        verify(sysUserRoleMapper, never()).insert(any(SysUserRole.class));
    }

    @Test
    void assignRolesReactivatesDeletedRelationAndDeletesRemovedActiveRelation() {
        UserService userService = new UserService(sysUserMapper, sysUserRoleMapper, sysRolePermissionMapper,
                sysUserPermissionMapper, sysRoleMapper, sysPermissionMapper);

        SysUser user = new SysUser();
        user.setId(100L);
        when(sysUserMapper.selectById(100L)).thenReturn(user);

        SysRole activeRole = new SysRole();
        activeRole.setId(1L);
        activeRole.setStatus(1);
        when(sysRoleMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(activeRole));

        SysUserRole deletedRelation = new SysUserRole();
        deletedRelation.setId(10L);
        deletedRelation.setUserId(100L);
        deletedRelation.setRoleId(1L);
        deletedRelation.setDeleted(1);

        SysUserRole activeRemovedRelation = new SysUserRole();
        activeRemovedRelation.setId(11L);
        activeRemovedRelation.setUserId(100L);
        activeRemovedRelation.setRoleId(2L);
        activeRemovedRelation.setDeleted(0);

        when(sysUserRoleMapper.selectAllByUserId(100L)).thenReturn(List.of(deletedRelation, activeRemovedRelation));

        org.liangyou.modules.user.dto.UserRoleAssignRequest request = new org.liangyou.modules.user.dto.UserRoleAssignRequest();
        request.setRoleIds(List.of(1L));

        userService.assignRoles(100L, request);

        verify(sysUserRoleMapper, times(2)).updateById(any(SysUserRole.class));
        verify(sysUserRoleMapper, never()).insert(any(SysUserRole.class));
    }

    @Test
    void assignUserPermissionsRejectsDisabledPermissionIdsBeforePersisting() {
        UserService userService = new UserService(sysUserMapper, sysUserRoleMapper, sysRolePermissionMapper,
                sysUserPermissionMapper, sysRoleMapper, sysPermissionMapper);

        SysUser user = new SysUser();
        user.setId(100L);
        when(sysUserMapper.selectById(100L)).thenReturn(user);

        SysPermission active = new SysPermission();
        active.setId(201L);
        active.setStatus(1);
        SysPermission disabled = new SysPermission();
        disabled.setId(202L);
        disabled.setStatus(0);
        when(sysPermissionMapper.selectBatchIds(List.of(201L, 202L))).thenReturn(List.of(active, disabled));

        org.liangyou.modules.user.dto.UserPermissionAssignRequest request = new org.liangyou.modules.user.dto.UserPermissionAssignRequest();
        request.setGrantPermissionIds(List.of(201L));
        request.setRevokePermissionIds(List.of(202L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.assignUserPermissions(100L, request));
        assertEquals(400, exception.getCode());
        assertEquals("permission-disabled", exception.getMessage());
        verify(sysUserPermissionMapper, never()).insert(any(SysUserPermission.class));
        verify(sysUserPermissionMapper, never()).updateById(any(SysUserPermission.class));
    }

    @Test
    void assignUserPermissionsRejectsContradictoryGrantAndRevokeIds() {
        UserService userService = new UserService(sysUserMapper, sysUserRoleMapper, sysRolePermissionMapper,
                sysUserPermissionMapper, sysRoleMapper, sysPermissionMapper);

        SysUser user = new SysUser();
        user.setId(100L);
        when(sysUserMapper.selectById(100L)).thenReturn(user);

        SysPermission active = new SysPermission();
        active.setId(201L);
        active.setStatus(1);
        when(sysPermissionMapper.selectBatchIds(List.of(201L))).thenReturn(List.of(active));

        org.liangyou.modules.user.dto.UserPermissionAssignRequest request = new org.liangyou.modules.user.dto.UserPermissionAssignRequest();
        request.setGrantPermissionIds(List.of(201L));
        request.setRevokePermissionIds(List.of(201L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.assignUserPermissions(100L, request));
        assertEquals(400, exception.getCode());
        assertEquals("permission-conflict", exception.getMessage());
        verify(sysUserPermissionMapper, never()).insert(any(SysUserPermission.class));
        verify(sysUserPermissionMapper, never()).updateById(any(SysUserPermission.class));
    }

    @Test
    void assignUserPermissionsRejectsMissingPermissionIdsBeforePersisting() {
        UserService userService = new UserService(sysUserMapper, sysUserRoleMapper, sysRolePermissionMapper,
                sysUserPermissionMapper, sysRoleMapper, sysPermissionMapper);

        SysUser user = new SysUser();
        user.setId(100L);
        when(sysUserMapper.selectById(100L)).thenReturn(user);

        SysPermission active = new SysPermission();
        active.setId(201L);
        active.setStatus(1);
        when(sysPermissionMapper.selectBatchIds(List.of(201L, 202L))).thenReturn(List.of(active));

        org.liangyou.modules.user.dto.UserPermissionAssignRequest request = new org.liangyou.modules.user.dto.UserPermissionAssignRequest();
        request.setGrantPermissionIds(List.of(201L));
        request.setRevokePermissionIds(List.of(202L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.assignUserPermissions(100L, request));
        assertEquals(404, exception.getCode());
        assertEquals("permission-not-found", exception.getMessage());
        verify(sysUserPermissionMapper, never()).insert(any(SysUserPermission.class));
        verify(sysUserPermissionMapper, never()).updateById(any(SysUserPermission.class));
    }

    @Test
    void assignUserPermissionsReactivatesDeletedGrantRelation() {
        UserService userService = new UserService(sysUserMapper, sysUserRoleMapper, sysRolePermissionMapper,
                sysUserPermissionMapper, sysRoleMapper, sysPermissionMapper);

        SysUser user = new SysUser();
        user.setId(100L);
        when(sysUserMapper.selectById(100L)).thenReturn(user);

        SysPermission active = new SysPermission();
        active.setId(201L);
        active.setStatus(1);
        when(sysPermissionMapper.selectBatchIds(List.of(201L))).thenReturn(List.of(active));

        SysUserPermission deletedGrant = new SysUserPermission();
        deletedGrant.setId(30L);
        deletedGrant.setUserId(100L);
        deletedGrant.setPermissionId(201L);
        deletedGrant.setGrantType(1);
        deletedGrant.setDeleted(1);
        when(sysUserPermissionMapper.selectAllByUserId(100L)).thenReturn(List.of(deletedGrant));

        org.liangyou.modules.user.dto.UserPermissionAssignRequest request = new org.liangyou.modules.user.dto.UserPermissionAssignRequest();
        request.setGrantPermissionIds(List.of(201L));

        userService.assignUserPermissions(100L, request);

        verify(sysUserPermissionMapper, times(1)).updateById(any(SysUserPermission.class));
        verify(sysUserPermissionMapper, never()).insert(any(SysUserPermission.class));
    }

    @Test
    void getUserPermissionsUsesActiveRolesOnly() {
        UserService userService = new UserService(sysUserMapper, sysUserRoleMapper, sysRolePermissionMapper,
                sysUserPermissionMapper, sysRoleMapper, sysPermissionMapper);

        SysUser user = new SysUser();
        user.setId(100L);
        user.setUsername("warehouse01");
        when(sysUserMapper.selectById(100L)).thenReturn(user);

        SysUserRole activeRole = new SysUserRole();
        activeRole.setRoleId(1L);
        SysUserRole inactiveRole = new SysUserRole();
        inactiveRole.setRoleId(2L);
        when(sysUserRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(activeRole, inactiveRole));

        SysRole active = new SysRole();
        active.setId(1L);
        active.setRoleCode("WAREHOUSE");
        active.setStatus(1);
        SysRole inactive = new SysRole();
        inactive.setId(2L);
        inactive.setRoleCode("OLD_ROLE");
        inactive.setStatus(0);
        when(sysRoleMapper.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(active, inactive));

        SysRolePermission relation = new SysRolePermission();
        relation.setPermissionId(201L);
        when(sysRolePermissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(relation));
        when(sysUserPermissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        UserPermissionResponse response = userService.getUserPermissions(100L);

        assertEquals(List.of(1L), response.getRoleIds());
        assertEquals(List.of(201L), response.getPermissionIds());
    }
}
