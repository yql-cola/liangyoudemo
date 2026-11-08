-- 2026-10-21 00:14 Asia/Shanghai
-- 一次性迁移：将角色上有效的 sale:order:entry 关联转为 sale:order:create。
-- 适用环境：已执行销售权限补丁、同时存在两个权限编码的已有数据库。
-- 手动执行；先备份 sys_role_permission，并确认下方第一条查询返回两条未删除且启用的权限。
-- 不处理 sys_user_permission 中的用户直接授权或撤销记录。

SELECT id, permission_code, status, deleted
FROM sys_permission
WHERE permission_code IN ('sale:order:entry', 'sale:order:create');

-- 临时保存待迁移的角色，以便验证。重跑时已迁移的角色不会再次进入此表。
DROP TEMPORARY TABLE IF EXISTS tmp_sale_entry_roles;
CREATE TEMPORARY TABLE tmp_sale_entry_roles (
    role_id BIGINT PRIMARY KEY,
    created_by BIGINT NULL
) ENGINE=InnoDB;

INSERT INTO tmp_sale_entry_roles (role_id, created_by)
SELECT relation.role_id, relation.created_by
FROM sys_role_permission AS relation
JOIN sys_permission AS permission ON permission.id = relation.permission_id
WHERE permission.permission_code = 'sale:order:entry'
  AND permission.deleted = 0
  AND relation.deleted = 0;

SELECT COUNT(*) AS roles_to_migrate FROM tmp_sale_entry_roles;

START TRANSACTION;

-- 唯一键 (role_id, permission_id) 已存在时恢复软删除关联；否则新增关联。
INSERT INTO sys_role_permission (role_id, permission_id, created_by, deleted)
SELECT old_role.role_id, permission.id, old_role.created_by, 0
FROM tmp_sale_entry_roles AS old_role
JOIN sys_permission AS permission
  ON permission.permission_code = 'sale:order:create'
 AND permission.status = 1
 AND permission.deleted = 0
ON DUPLICATE KEY UPDATE deleted = 0;

UPDATE sys_role_permission AS relation
JOIN tmp_sale_entry_roles AS old_role ON old_role.role_id = relation.role_id
JOIN sys_permission AS permission ON permission.id = relation.permission_id
JOIN sys_permission AS replacement
  ON replacement.permission_code = 'sale:order:create'
 AND replacement.status = 1
 AND replacement.deleted = 0
JOIN sys_role_permission AS new_relation
  ON new_relation.role_id = relation.role_id
 AND new_relation.permission_id = replacement.id
 AND new_relation.deleted = 0
SET relation.deleted = 1
WHERE permission.permission_code = 'sale:order:entry'
  AND relation.deleted = 0;

COMMIT;

-- 校验：以下两项都应为 0；否则停止后续 update.sql 变更并排查数据。
SELECT COUNT(*) AS missing_create_grants
FROM tmp_sale_entry_roles AS old_role
LEFT JOIN sys_permission AS permission
  ON permission.permission_code = 'sale:order:create' AND permission.deleted = 0
LEFT JOIN sys_role_permission AS relation
  ON relation.role_id = old_role.role_id
 AND relation.permission_id = permission.id
 AND relation.deleted = 0
WHERE relation.id IS NULL;

SELECT COUNT(*) AS remaining_entry_grants
FROM tmp_sale_entry_roles AS old_role
JOIN sys_permission AS permission
  ON permission.permission_code = 'sale:order:entry' AND permission.deleted = 0
JOIN sys_role_permission AS relation
  ON relation.role_id = old_role.role_id
 AND relation.permission_id = permission.id
 AND relation.deleted = 0;

DROP TEMPORARY TABLE tmp_sale_entry_roles;
