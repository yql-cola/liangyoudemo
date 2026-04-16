package org.liangyou.modules.auth.service;

import lombok.RequiredArgsConstructor;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.auth.dto.LoginRequest;
import org.liangyou.modules.auth.vo.CurrentUserResponse;
import org.liangyou.modules.auth.vo.LoginResponse;
import org.liangyou.modules.user.entity.SysUser;
import org.liangyou.modules.user.service.UserService;
import org.liangyou.security.AuthenticatedUserPrincipal;
import org.liangyou.security.JwtTokenService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final StringRedisTemplate stringRedisTemplate;

    public LoginResponse login(LoginRequest request) {
        SysUser user = userService.findByUsername(request.getUsername());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        if (!Objects.equals(user.getPassword(), request.getPassword())) {
            throw new BusinessException(400, "用户名或密码错误");
        }

        UserService.LoginAuthorities loginAuthorities = userService.loadLoginAuthorities(user.getId());
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                loginAuthorities.getRoles(),
                loginAuthorities.getPermissions());
        String token = jwtTokenService.issueToken(principal);
        return toLoginResponse(token, principal);
    }

    public CurrentUserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new BusinessException(401, "未登录或 Token 无效");
        }
        return toCurrentUserResponse(principal);
    }

    public void logout(String authorizationHeader) {
        String token = resolveBearerToken(authorizationHeader);
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(401, "未登录或 Token 无效");
        }
        long remainingTtlSeconds;
        try {
            remainingTtlSeconds = jwtTokenService.getRemainingTtlSeconds(token);
        } catch (RuntimeException ex) {
            throw new BusinessException(401, "未登录或 Token 无效");
        }
        if (remainingTtlSeconds <= 0) {
            return;
        }
        stringRedisTemplate.opsForValue().set(
                jwtTokenService.buildRevokedTokenKey(token),
                "revoked",
                java.time.Duration.ofSeconds(remainingTtlSeconds));
    }

    private LoginResponse toLoginResponse(String token, AuthenticatedUserPrincipal principal) {
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtTokenService.getExpirationSeconds());
        response.setUserInfo(toCurrentUserResponse(principal));
        return response;
    }

    private CurrentUserResponse toCurrentUserResponse(AuthenticatedUserPrincipal principal) {
        CurrentUserResponse response = new CurrentUserResponse();
        response.setId(principal.getId());
        response.setUsername(principal.getUsername());
        response.setRealName(principal.getRealName());
        response.setRoles(principal.getRoles());
        response.setPermissions(principal.getPermissions());
        return response;
    }

    private String resolveBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring("Bearer ".length());
    }
}
