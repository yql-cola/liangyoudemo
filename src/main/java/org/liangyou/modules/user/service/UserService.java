package org.liangyou.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.PageResponse;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.user.dto.RoleCreateRequest;
import org.liangyou.modules.user.dto.RolePermissionAssignRequest;
import org.liangyou.modules.user.dto.RoleQueryRequest;
import org.liangyou.modules.user.dto.RoleUpdateRequest;
import org.liangyou.modules.user.dto.UserCreateRequest;
import org.liangyou.modules.user.dto.UserPermissionAssignRequest;
import org.liangyou.modules.user.dto.UserQueryRequest;
import org.liangyou.modules.user.dto.UserRoleAssignRequest;
import org.liangyou.modules.user.dto.UserUpdateRequest;
import org.liangyou.modules.user.entity.SysRolePermission;
import org.liangyou.modules.user.entity.SysUser;
import org.liangyou.modules.user.entity.SysUserPermission;
import org.liangyou.modules.user.entity.SysUserRole;
import org.liangyou.modules.user.entity.SysRole;
import org.liangyou.modules.user.entity.SysPermission;
import org.liangyou.modules.user.mapper.SysRolePermissionMapper;
import org.liangyou.modules.user.mapper.SysRoleMapper;
import org.liangyou.modules.user.mapper.SysPermissionMapper;
import org.liangyou.modules.user.mapper.SysUserMapper;
import org.liangyou.modules.user.mapper.SysUserPermissionMapper;
import org.liangyou.modules.user.mapper.SysUserRoleMapper;
import org.liangyou.modules.user.vo.MenuTreeResponse;
import org.liangyou.modules.user.vo.RoleResponse;
import org.liangyou.modules.user.vo.UserDetailResponse;
import org.liangyou.modules.user.vo.UserPermissionResponse;
import org.liangyou.security.AuthenticatedUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysUserPermissionMapper sysUserPermissionMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;

    @Transactional
    public RoleResponse createRole(RoleCreateRequest request) {
        ensureRoleCodeAvailable(request.getRoleCode(), null);
        SysRole role = new SysRole();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        role.setRemark(request.getRemark());
        sysRoleMapper.insert(role);
        return toRoleResponse(role);
    }

    @Transactional
    public RoleResponse updateRole(Long id, RoleUpdateRequest request) {
        SysRole role = requireRole(id);
        ensureRoleCodeAvailable(request.getRoleCode(), id);
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setStatus(request.getStatus());
        role.setRemark(request.getRemark());
        sysRoleMapper.updateById(role);
        return toRoleResponse(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        requireRole(id);
        sysRoleMapper.deleteById(id);
    }

    public List<RoleResponse> listRoles(RoleQueryRequest request) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (request != null) {
            wrapper.like(StringUtils.hasText(request.getRoleName()), SysRole::getRoleName, request.getRoleName())
                    .like(StringUtils.hasText(request.getRoleCode()), SysRole::getRoleCode, request.getRoleCode())
                    .eq(request.getStatus() != null, SysRole::getStatus, request.getStatus());
        }
        wrapper.orderByDesc(SysRole::getId);
        return sysRoleMapper.selectList(wrapper).stream()
                .map(this::toRoleResponse)
                .toList();
    }

    @Transactional
    public void assignRolePermissions(Long roleId, RolePermissionAssignRequest request) {
        requireRole(roleId);
        List<Long> permissionIds = uniqueIds(request.getPermissionIds());
        validatePermissionIds(permissionIds);
        Map<Long, SysRolePermission> existingRelations = sysRolePermissionMapper.selectAllByRoleId(roleId).stream()
                .collect(Collectors.toMap(SysRolePermission::getPermissionId, relation -> relation, (left, right) -> left));
        Set<Long> desiredPermissionIds = new LinkedHashSet<>(permissionIds);
        for (Long permissionId : desiredPermissionIds) {
            SysRolePermission existing = existingRelations.get(permissionId);
            if (existing == null) {
                SysRolePermission relation = new SysRolePermission();
                relation.setRoleId(roleId);
                relation.setPermissionId(permissionId);
                sysRolePermissionMapper.insert(relation);
                continue;
            }
            if (isDeleted(existing)) {
                existing.setDeleted(0);
                sysRolePermissionMapper.updateById(existing);
            }
        }
        for (SysRolePermission existing : existingRelations.values()) {
            if (!desiredPermissionIds.contains(existing.getPermissionId()) && !isDeleted(existing)) {
                existing.setDeleted(1);
                sysRolePermissionMapper.updateById(existing);
            }
        }
    }

    public List<MenuTreeResponse> getMenuTree() {
        AuthenticatedUserPrincipal principal = requireAuthenticatedPrincipal();
        List<SysPermission> menus = sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getPermissionType, 1)
                .eq(SysPermission::getStatus, 1)
                .orderByAsc(SysPermission::getId));
        if (principal.getRoles().contains("SUPER_ADMIN")) {
            return buildMenuTree(menus);
        }

        Set<String> grantedPermissions = new HashSet<>(principal.getPermissions());
        Map<Long, SysPermission> menuMap = menus.stream()
                .collect(Collectors.toMap(SysPermission::getId, menu -> menu, (left, right) -> left, LinkedHashMap::new));
        Set<Long> visibleMenuIds = new LinkedHashSet<>();
        for (SysPermission menu : menus) {
            if (StringUtils.hasText(menu.getPermissionCode()) && grantedPermissions.contains(menu.getPermissionCode())) {
                includeMenuAndAncestors(menu, menuMap, visibleMenuIds);
            }
        }
        List<SysPermission> visibleMenus = menus.stream()
                .filter(menu -> visibleMenuIds.contains(menu.getId()))
                .toList();
        return buildMenuTree(visibleMenus);
    }

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
        List<Long> roleIds = uniqueIds(request.getRoleIds());
        validateRoleIds(roleIds);
        Map<Long, SysUserRole> existingRelations = sysUserRoleMapper.selectAllByUserId(userId).stream()
                .collect(Collectors.toMap(SysUserRole::getRoleId, relation -> relation, (left, right) -> left));
        Set<Long> desiredRoleIds = new LinkedHashSet<>(roleIds);
        for (Long roleId : desiredRoleIds) {
            SysUserRole existing = existingRelations.get(roleId);
            if (existing == null) {
                SysUserRole relation = new SysUserRole();
                relation.setUserId(userId);
                relation.setRoleId(roleId);
                sysUserRoleMapper.insert(relation);
                continue;
            }
            if (isDeleted(existing)) {
                existing.setDeleted(0);
                sysUserRoleMapper.updateById(existing);
            }
        }
        for (SysUserRole existing : existingRelations.values()) {
            if (!desiredRoleIds.contains(existing.getRoleId()) && !isDeleted(existing)) {
                existing.setDeleted(1);
                sysUserRoleMapper.updateById(existing);
            }
        }
    }

    @Transactional
    public void assignUserPermissions(Long userId, UserPermissionAssignRequest request) {
        requireUser(userId);
        List<Long> permissionIds = new ArrayList<>();
        if (request.getGrantPermissionIds() != null) {
            permissionIds.addAll(request.getGrantPermissionIds());
        }
        if (request.getRevokePermissionIds() != null) {
            permissionIds.addAll(request.getRevokePermissionIds());
        }
        validatePermissionIds(uniqueIds(permissionIds));
        Set<Long> grantIds = new LinkedHashSet<>(uniqueIds(request.getGrantPermissionIds()));
        Set<Long> revokeIds = new LinkedHashSet<>(uniqueIds(request.getRevokePermissionIds()));
        grantIds.retainAll(revokeIds);
        if (!grantIds.isEmpty()) {
            throw new BusinessException(400, "permission-conflict");
        }
        if (request.getGrantPermissionIds() != null) {
            upsertUserPermissions(userId, uniqueIds(request.getGrantPermissionIds()), 1);
        }
        if (request.getRevokePermissionIds() != null) {
            upsertUserPermissions(userId, uniqueIds(request.getRevokePermissionIds()), 0);
        }
    }

    public UserPermissionResponse getUserPermissions(Long userId) {
        requireUser(userId);
        List<Long> roleIds = loadActiveRoleIds(listRoleIds(userId));
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

    public SysUser findByUsername(String username) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("limit 1"));
    }

    public LoginAuthorities loadLoginAuthorities(Long userId) {
        SysUser user = requireUser(userId);
        if (user.getIsSuperAdmin() != null && user.getIsSuperAdmin() == 1) {
            List<String> activeSuperAdminPermissions = sysPermissionMapper.selectList(
                            new LambdaQueryWrapper<SysPermission>().eq(SysPermission::getStatus, 1))
                    .stream()
                    .map(SysPermission::getPermissionCode)
                    .filter(StringUtils::hasText)
                    .toList();
            return new LoginAuthorities(List.of("SUPER_ADMIN"), activeSuperAdminPermissions);
        }

        List<Long> activeRoleIds = loadActiveRoleIds(listRoleIds(userId));
        List<String> roles = loadRoleCodes(activeRoleIds);
        Set<Long> permissionIds = new LinkedHashSet<>();
        if (!activeRoleIds.isEmpty()) {
            permissionIds.addAll(sysRolePermissionMapper.selectList(
                            new LambdaQueryWrapper<SysRolePermission>().in(SysRolePermission::getRoleId, activeRoleIds))
                    .stream()
                    .map(SysRolePermission::getPermissionId)
                    .collect(Collectors.toSet()));
        }

        List<SysUserPermission> userPermissions = sysUserPermissionMapper.selectList(
                new LambdaQueryWrapper<SysUserPermission>().eq(SysUserPermission::getUserId, userId));
        userPermissions.stream()
                .filter(item -> item.getGrantType() != null && item.getGrantType() == 1)
                .map(SysUserPermission::getPermissionId)
                .forEach(permissionIds::add);
        userPermissions.stream()
                .filter(item -> item.getGrantType() != null && item.getGrantType() == 0)
                .map(SysUserPermission::getPermissionId)
                .forEach(permissionIds::remove);

        List<String> permissions = loadPermissionCodes(new ArrayList<>(permissionIds));
        return new LoginAuthorities(roles, permissions);
    }

    private SysUser requireUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "user-not-found");
        }
        return user;
    }

    private SysRole requireRole(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(404, "role-not-found");
        }
        return role;
    }

    private void ensureRoleCodeAvailable(String roleCode, Long excludedRoleId) {
        boolean conflict = sysRoleMapper.selectAllByRoleCode(roleCode).stream()
                .anyMatch(role -> excludedRoleId == null || !Objects.equals(role.getId(), excludedRoleId));
        if (conflict) {
            throw new BusinessException(400, "role-code-exists");
        }
    }

    private void validatePermissionIds(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }
        List<SysPermission> permissions = sysPermissionMapper.selectBatchIds(permissionIds);
        Map<Long, SysPermission> permissionMap = permissions.stream()
                .collect(Collectors.toMap(SysPermission::getId, permission -> permission));
        for (Long permissionId : permissionIds) {
            SysPermission permission = permissionMap.get(permissionId);
            if (permission == null) {
                throw new BusinessException(404, "permission-not-found");
            }
            if (permission.getStatus() == null || permission.getStatus() != 1) {
                throw new BusinessException(400, "permission-disabled");
            }
        }
    }

    private void validateRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
        Map<Long, SysRole> roleMap = roles.stream()
                .collect(Collectors.toMap(SysRole::getId, role -> role));
        for (Long roleId : roleIds) {
            SysRole role = roleMap.get(roleId);
            if (role == null) {
                throw new BusinessException(404, "role-not-found");
            }
            if (role.getStatus() == null || role.getStatus() != 1) {
                throw new BusinessException(400, "role-disabled");
            }
        }
    }

    private RoleResponse toRoleResponse(SysRole role) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setRoleCode(role.getRoleCode());
        response.setRoleName(role.getRoleName());
        response.setStatus(role.getStatus());
        response.setRemark(role.getRemark());
        return response;
    }

    private AuthenticatedUserPrincipal requireAuthenticatedPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new BusinessException(401, "未登录或 Token 无效");
        }
        return principal;
    }

    private void includeMenuAndAncestors(SysPermission menu,
                                         Map<Long, SysPermission> menuMap,
                                         Set<Long> visibleMenuIds) {
        Long currentId = menu.getId();
        while (currentId != null && currentId != 0 && visibleMenuIds.add(currentId)) {
            SysPermission current = menuMap.get(currentId);
            if (current == null) {
                break;
            }
            currentId = current.getParentId();
            if (currentId == null) {
                break;
            }
        }
    }

    private List<MenuTreeResponse> buildMenuTree(List<SysPermission> menus) {
        Map<Long, List<SysPermission>> byParentId = menus.stream()
                .collect(Collectors.groupingBy(menu -> normalizeParentId(menu.getParentId()),
                        LinkedHashMap::new, Collectors.toList()));
        return buildMenuTreeChildren(0L, byParentId);
    }

    private List<MenuTreeResponse> buildMenuTreeChildren(Long parentId, Map<Long, List<SysPermission>> byParentId) {
        return byParentId.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparing(SysPermission::getId))
                .map(menu -> {
                    MenuTreeResponse response = new MenuTreeResponse();
                    response.setId(menu.getId());
                    response.setName(menu.getPermissionName());
                    response.setPath(menu.getPath());
                    response.setChildren(buildMenuTreeChildren(menu.getId(), byParentId));
                    return response;
                })
                .toList();
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    private List<Long> listRoleIds(Long userId) {
        return sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();
    }

    private List<String> loadRoleCodes(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return sysRoleMapper.selectBatchIds(roleIds).stream()
                .filter(role -> role.getStatus() != null && role.getStatus() == 1)
                .map(SysRole::getRoleCode)
                .filter(StringUtils::hasText)
                .toList();
    }

    private List<String> loadPermissionCodes(List<Long> permissionIds) {
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return sysPermissionMapper.selectBatchIds(permissionIds).stream()
                .filter(permission -> permission.getStatus() != null && permission.getStatus() == 1)
                .map(SysPermission::getPermissionCode)
                .filter(StringUtils::hasText)
                .toList();
    }

    private List<Long> loadActiveRoleIds(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return sysRoleMapper.selectBatchIds(roleIds).stream()
                .filter(role -> role.getStatus() != null && role.getStatus() == 1)
                .map(SysRole::getId)
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
        Map<Long, SysUserPermission> existingRelations = sysUserPermissionMapper.selectAllByUserId(userId).stream()
                .collect(Collectors.toMap(SysUserPermission::getPermissionId, relation -> relation, (left, right) -> left));
        for (Long permissionId : permissionIds) {
            SysUserPermission existing = existingRelations.get(permissionId);
            if (existing == null) {
                SysUserPermission relation = new SysUserPermission();
                relation.setUserId(userId);
                relation.setPermissionId(permissionId);
                relation.setGrantType(grantType);
                sysUserPermissionMapper.insert(relation);
                continue;
            }
            if (isDeleted(existing) || !Objects.equals(existing.getGrantType(), grantType)) {
                existing.setGrantType(grantType);
                existing.setDeleted(0);
                sysUserPermissionMapper.updateById(existing);
            }
        }
    }

    private List<Long> uniqueIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    private boolean isDeleted(SysUserRole relation) {
        return relation.getDeleted() != null && relation.getDeleted() == 1;
    }

    private boolean isDeleted(SysRolePermission relation) {
        return relation.getDeleted() != null && relation.getDeleted() == 1;
    }

    private boolean isDeleted(SysUserPermission relation) {
        return relation.getDeleted() != null && relation.getDeleted() == 1;
    }

    public static class LoginAuthorities {
        private final List<String> roles;
        private final List<String> permissions;

        public LoginAuthorities(List<String> roles, List<String> permissions) {
            this.roles = roles;
            this.permissions = permissions;
        }

        public List<String> getRoles() {
            return roles;
        }

        public List<String> getPermissions() {
            return permissions;
        }
    }
}
