package org.liangyou.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.PageResponse;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.user.dto.RolePermissionAssignRequest;
import org.liangyou.modules.user.dto.UserCreateRequest;
import org.liangyou.modules.user.dto.UserPermissionAssignRequest;
import org.liangyou.modules.user.dto.UserQueryRequest;
import org.liangyou.modules.user.dto.UserRoleAssignRequest;
import org.liangyou.modules.user.dto.UserUpdateRequest;
import org.liangyou.modules.user.entity.SysRolePermission;
import org.liangyou.modules.user.entity.SysUser;
import org.liangyou.modules.user.entity.SysUserPermission;
import org.liangyou.modules.user.entity.SysUserRole;
import org.liangyou.modules.user.mapper.SysRolePermissionMapper;
import org.liangyou.modules.user.mapper.SysUserMapper;
import org.liangyou.modules.user.mapper.SysUserPermissionMapper;
import org.liangyou.modules.user.mapper.SysUserRoleMapper;
import org.liangyou.modules.user.vo.UserDetailResponse;
import org.liangyou.modules.user.vo.UserPermissionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysUserPermissionMapper sysUserPermissionMapper;

    @Transactional
    public Long createUser(UserCreateRequest request) {
        long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException(400, "username-exists");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setWarehouseId(request.getWarehouseId());
        user.setStatus(request.getStatus());
        user.setIsSuperAdmin(request.getIsSuperAdmin());
        sysUserMapper.insert(user);
        return user.getId();
    }

    @Transactional
    public void updateUser(Long id, UserUpdateRequest request) {
        SysUser user = requireUser(id);
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setWarehouseId(request.getWarehouseId());
        user.setStatus(request.getStatus());
        user.setIsSuperAdmin(request.getIsSuperAdmin());
        sysUserMapper.updateById(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        requireUser(id);
        sysUserMapper.deleteById(id);
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        sysUserPermissionMapper.delete(new LambdaQueryWrapper<SysUserPermission>().eq(SysUserPermission::getUserId, id));
    }

    public UserDetailResponse getUserDetail(Long id) {
        SysUser user = requireUser(id);
        UserDetailResponse response = toUserDetail(user);
        response.setRoleIds(listRoleIds(id));
        return response;
    }

    public PageResponse<UserDetailResponse> queryUsers(UserQueryRequest request) {
        Page<SysUser> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(request.getUsername()), SysUser::getUsername, request.getUsername())
                .like(StringUtils.hasText(request.getRealName()), SysUser::getRealName, request.getRealName())
                .eq(request.getWarehouseId() != null, SysUser::getWarehouseId, request.getWarehouseId())
                .eq(request.getStatus() != null, SysUser::getStatus, request.getStatus())
                .orderByDesc(SysUser::getId);
        Page<SysUser> result = sysUserMapper.selectPage(page, wrapper);
        List<UserDetailResponse> list = result.getRecords().stream()
                .map(this::toUserDetail)
                .toList();
        return new PageResponse<>(list, result.getTotal());
    }

    @Transactional
    public void assignRoles(Long userId, UserRoleAssignRequest request) {
        requireUser(userId);
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (request.getRoleIds() == null) {
            return;
        }
        for (Long roleId : request.getRoleIds()) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            sysUserRoleMapper.insert(relation);
        }
    }

    @Transactional
    public void assignUserPermissions(Long userId, UserPermissionAssignRequest request) {
        requireUser(userId);
        if (request.getGrantPermissionIds() != null) {
            upsertUserPermissions(userId, request.getGrantPermissionIds(), 1);
        }
        if (request.getRevokePermissionIds() != null) {
            upsertUserPermissions(userId, request.getRevokePermissionIds(), 0);
        }
    }

    @Transactional
    public void assignRolePermissions(Long roleId, RolePermissionAssignRequest request) {
        sysRolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
        for (Long permissionId : request.getPermissionIds()) {
            SysRolePermission relation = new SysRolePermission();
            relation.setRoleId(roleId);
            relation.setPermissionId(permissionId);
            sysRolePermissionMapper.insert(relation);
        }
    }

    public UserPermissionResponse getUserPermissions(Long userId) {
        requireUser(userId);
        List<Long> roleIds = listRoleIds(userId);
        Set<Long> permissions = new LinkedHashSet<>();
        if (!roleIds.isEmpty()) {
            permissions.addAll(sysRolePermissionMapper.selectList(
                            new LambdaQueryWrapper<SysRolePermission>().in(SysRolePermission::getRoleId, roleIds))
                    .stream()
                    .map(SysRolePermission::getPermissionId)
                    .collect(Collectors.toSet()));
        }

        List<SysUserPermission> userPermissions = sysUserPermissionMapper.selectList(
                new LambdaQueryWrapper<SysUserPermission>().eq(SysUserPermission::getUserId, userId));
        userPermissions.stream()
                .filter(item -> item.getGrantType() != null && item.getGrantType() == 1)
                .map(SysUserPermission::getPermissionId)
                .forEach(permissions::add);
        userPermissions.stream()
                .filter(item -> item.getGrantType() != null && item.getGrantType() == 0)
                .map(SysUserPermission::getPermissionId)
                .forEach(permissions::remove);

        UserPermissionResponse response = new UserPermissionResponse();
        response.setUserId(userId);
        response.setRoleIds(roleIds);
        response.setPermissionIds(new ArrayList<>(permissions));
        return response;
    }

    private SysUser requireUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "user-not-found");
        }
        return user;
    }

    private List<Long> listRoleIds(Long userId) {
        return sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();
    }

    private UserDetailResponse toUserDetail(SysUser user) {
        UserDetailResponse response = new UserDetailResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setWarehouseId(user.getWarehouseId());
        response.setStatus(user.getStatus());
        response.setIsSuperAdmin(user.getIsSuperAdmin());
        response.setRoleIds(Collections.emptyList());
        return response;
    }

    private void upsertUserPermissions(Long userId, List<Long> permissionIds, int grantType) {
        for (Long permissionId : permissionIds) {
            SysUserPermission existing = sysUserPermissionMapper.selectOne(new LambdaQueryWrapper<SysUserPermission>()
                    .eq(SysUserPermission::getUserId, userId)
                    .eq(SysUserPermission::getPermissionId, permissionId)
                    .last("limit 1"));
            if (existing == null) {
                SysUserPermission relation = new SysUserPermission();
                relation.setUserId(userId);
                relation.setPermissionId(permissionId);
                relation.setGrantType(grantType);
                sysUserPermissionMapper.insert(relation);
                continue;
            }
            sysUserPermissionMapper.update(null, new LambdaUpdateWrapper<SysUserPermission>()
                    .eq(SysUserPermission::getId, existing.getId())
                    .set(SysUserPermission::getGrantType, grantType)
                    .set(SysUserPermission::getDeleted, 0));
        }
    }
}
