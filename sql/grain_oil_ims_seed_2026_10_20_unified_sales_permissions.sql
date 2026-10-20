-- Fresh database only: run after grain_oil_ims_init.sql and the v1.1.0 permission seed.
-- Existing database upgrades receive these rows from the unified-sales update script.
USE grain_oil_ims;

INSERT INTO sys_permission(permission_code,permission_name,permission_type,parent_id,path,status)
VALUES('sale:order:entry','销售开单',1,(SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code='sale:menu') p),'/sale/new',1)
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name),path=VALUES(path);
UPDATE sys_permission SET permission_name='销售订单',path='/sale/out' WHERE permission_code='sale:order:list';
INSERT INTO sys_permission(permission_code,permission_name,permission_type,parent_id,status)
SELECT codes.code,codes.name,2,p.id,1 FROM (
  SELECT 'sale:order:confirm' code,'确认销售订单' name UNION ALL
  SELECT 'sale:delivery:start','开始配送' UNION ALL
  SELECT 'sale:delivery:complete','完成交付' UNION ALL
  SELECT 'sale:delivery:return','登记退回' UNION ALL
  SELECT 'sale:order:print','打印配送清单' UNION ALL
  SELECT 'sale:order:export','导出销售清单'
) codes CROSS JOIN sys_permission p WHERE p.permission_code='sale:order:list'
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name),parent_id=VALUES(parent_id);
