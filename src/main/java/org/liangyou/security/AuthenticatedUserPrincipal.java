package org.liangyou.security;

import java.util.List;

public class AuthenticatedUserPrincipal {

    private final Long id;
    private final String username;
    private final String realName;
    private final List<String> roles;
    private final List<String> permissions;

    public AuthenticatedUserPrincipal(Long id, String username, String realName, List<String> roles, List<String> permissions) {
        this.id = id;
        this.username = username;
        this.realName = realName;
        this.roles = List.copyOf(roles);
        this.permissions = List.copyOf(permissions);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRealName() {
        return realName;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }
}
