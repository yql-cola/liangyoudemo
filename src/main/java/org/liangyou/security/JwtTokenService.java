package org.liangyou.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtTokenService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String REVOKED_TOKEN_PREFIX = "auth:jwt:revoked:";

    private final ObjectMapper objectMapper;

    @Value("${security.jwt.secret:}")
    private String secret;

    @Value("${security.jwt.expiration-seconds:7200}")
    private long expirationSeconds;

    public JwtTokenService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void validateConfiguration() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("security.jwt.secret must be configured");
        }
        if (expirationSeconds <= 0) {
            throw new IllegalStateException("security.jwt.expiration-seconds must be greater than 0");
        }
    }

    public String issueToken(AuthenticatedUserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", principal.getUsername());
        claims.put("uid", principal.getId());
        claims.put("realName", principal.getRealName());
        claims.put("roles", principal.getRoles());
        claims.put("permissions", principal.getPermissions());
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());
        return sign(claims);
    }

    public JwtTokenClaims parseToken(String token) {
        JwtTokenClaims claims = parseTokenClaims(token);
        if (Instant.now().getEpochSecond() >= claims.expiresAt()) {
            throw new IllegalArgumentException("token-expired");
        }
        return claims;
    }

    public long getRemainingTtlSeconds(String token) {
        JwtTokenClaims claims = parseTokenClaims(token);
        long remaining = claims.expiresAt() - Instant.now().getEpochSecond();
        return Math.max(remaining, 0L);
    }

    public String buildRevokedTokenKey(String token) {
        return REVOKED_TOKEN_PREFIX + sha256Hex(token);
    }

    private JwtTokenClaims parseTokenClaims(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("invalid-token");
        }
        String signingInput = parts[0] + "." + parts[1];
        String expectedSignature = signRaw(signingInput);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("invalid-token");
        }

        Map<String, Object> claims = readClaims(parts[1]);
        return new JwtTokenClaims(
                toLong(claims.get("uid")),
                stringValue(claims.get("sub")),
                stringValue(claims.get("realName")),
                toStringList(claims.get("roles")),
                toStringList(claims.get("permissions")),
                toLong(claims.get("iat")),
                toLong(claims.get("exp"))
        );
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private String sign(Map<String, Object> claims) {
        try {
            String header = base64Url(objectMapper.writeValueAsString(Map.of("alg", "HS256", "typ", "JWT")));
            String payload = base64Url(objectMapper.writeValueAsString(claims));
            String signingInput = header + "." + payload;
            return signingInput + "." + signRaw(signingInput);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("jwt-serialization-failed", e);
        }
    }

    private String signRaw(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return base64Url(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("jwt-signing-failed", e);
        }
    }

    private Map<String, Object> readClaims(String payloadPart) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(payloadPart);
            return objectMapper.readValue(decoded, new TypeReference<Map<String, Object>>() { });
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid-token", e);
        }
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("sha256-not-available", e);
        }
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private List<String> toStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of(String.valueOf(value));
    }

    public record JwtTokenClaims(Long userId,
                                 String username,
                                 String realName,
                                 List<String> roles,
                                 List<String> permissions,
                                 long issuedAt,
                                 long expiresAt) {
    }
}
