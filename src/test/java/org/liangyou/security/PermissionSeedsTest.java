package org.liangyou.security;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PermissionSeedsTest {
    @Test void replayPreservesDisabledPermissionsAndExistingRoleGrants() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:permission_seeds;MODE=MySQL")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE sys_permission (id BIGINT AUTO_INCREMENT PRIMARY KEY, permission_code VARCHAR(100) UNIQUE, permission_name VARCHAR(100), permission_type INT, parent_id BIGINT, path VARCHAR(200), status INT)");
                statement.execute("CREATE TABLE sys_role_permission (role_id BIGINT, permission_id BIGINT)");
                statement.execute("INSERT INTO sys_permission (id,permission_code,permission_name,permission_type,parent_id,status) VALUES (1,'basic:product:list','Disabled product',1,0,0)");
                statement.execute("INSERT INTO sys_role_permission VALUES (11,1)");
            }
            // H2 accepts the MySQL upserts but uses a different transaction-start statement.
            String sql = Files.readString(Path.of("sql/grain_oil_ims_patch_2026_10_17_permissions.sql"))
                    .replace("START TRANSACTION;", "SET AUTOCOMMIT FALSE;");
            var patch = new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8));
            ScriptUtils.executeSqlScript(connection, patch);
            int firstCount;
            try (var result = connection.createStatement().executeQuery("SELECT COUNT(*) FROM sys_permission")) {
                assertTrue(result.next());
                firstCount = result.getInt(1);
                assertTrue(firstCount > 100);
            }
            ScriptUtils.executeSqlScript(connection, patch);
            try (var statement = connection.createStatement()) {
                try (var result = statement.executeQuery("SELECT COUNT(*) FROM sys_permission")) {
                    assertTrue(result.next());
                    assertEquals(firstCount, result.getInt(1));
                }
                try (var result = statement.executeQuery("SELECT status FROM sys_permission WHERE id=1 AND permission_code='basic:product:list'")) {
                    assertTrue(result.next());
                    assertEquals(0, result.getInt(1));
                }
                try (var result = statement.executeQuery("SELECT role_id,permission_id FROM sys_role_permission")) {
                    assertTrue(result.next());
                    assertEquals(11, result.getLong(1));
                    assertEquals(1, result.getLong(2));
                    assertFalse(result.next());
                }
                try (var result = statement.executeQuery("SELECT COUNT(*) FROM sys_permission WHERE permission_code IN ('system:user:grant-role','system:user:grant-permission','system:user:view-permission')")) {
                    assertTrue(result.next());
                    assertEquals(3, result.getInt(1));
                }
            }
        }
    }
}
