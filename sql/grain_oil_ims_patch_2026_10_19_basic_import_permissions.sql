-- Repeatable import permissions for the seven basic-data menus.
START TRANSACTION;
INSERT INTO sys_permission(permission_code, permission_name, permission_type, parent_id, status)
VALUES ('basic:product:import', '商品批量导入', 2, (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'basic:product:list') p), 1)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), parent_id = VALUES(parent_id);
INSERT INTO sys_permission(permission_code, permission_name, permission_type, parent_id, status)
VALUES ('basic:category:import', '商品分类批量导入', 2, (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'basic:category:list') p), 1)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), parent_id = VALUES(parent_id);
INSERT INTO sys_permission(permission_code, permission_name, permission_type, parent_id, status)
VALUES ('basic:unit:import', '单位批量导入', 2, (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'basic:unit:list') p), 1)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), parent_id = VALUES(parent_id);
INSERT INTO sys_permission(permission_code, permission_name, permission_type, parent_id, status)
VALUES ('basic:unit-convert:import', '单位换算批量导入', 2, (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'basic:unit-convert:list') p), 1)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), parent_id = VALUES(parent_id);
INSERT INTO sys_permission(permission_code, permission_name, permission_type, parent_id, status)
VALUES ('basic:warehouse:import', '仓库批量导入', 2, (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'basic:warehouse:list') p), 1)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), parent_id = VALUES(parent_id);
INSERT INTO sys_permission(permission_code, permission_name, permission_type, parent_id, status)
VALUES ('basic:supplier:import', '供应商批量导入', 2, (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'basic:supplier:list') p), 1)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), parent_id = VALUES(parent_id);
INSERT INTO sys_permission(permission_code, permission_name, permission_type, parent_id, status)
VALUES ('basic:customer:import', '客户批量导入', 2, (SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'basic:customer:list') p), 1)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), parent_id = VALUES(parent_id);
COMMIT;
