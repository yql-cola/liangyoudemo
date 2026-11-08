-- 后续常规 SQL 变更统一追加在本文件末尾。
-- 每段变更需注明 Asia/Shanghai 日期时间、原因、影响对象和执行注意事项。
-- 已执行段落保留原样；历史数据迁移按 AGENTS.md 单独提供脚本，由用户手动执行。

-- 2026-10-20 10:18:19 Asia/Shanghai
-- 原因：上线统一销售开单、库存占用、配送状态及销售清单导出功能。
-- 影响对象：biz_sale_out、biz_sale_out_item、新表 sale_order_reservation、sale_order_event、sys_permission。
-- 执行顺序：已有旧版库先运行 scripts/upgrade-v1.1.0.sh，v1.1.0 库再运行 scripts/upgrade-unified-sales.sh；
--           全新库依次运行 init.sql、10_17_permissions.sql、10_19_basic_import_permissions.sql、10_20_unified_sales_permissions.sql。
-- 注意事项：本次不迁移旧销售单。执行前检查 SELECT COUNT(*) FROM grain_oil_ims.biz_sale_out；必须为 0，
--           否则补丁会主动报错停止。保留其他基础资料数据；备份数据库后手动执行一次，不得重复执行。
-- 校验：检查新增列及 COLUMN_COMMENT，确认两个新表均存在 AUTO_INCREMENT 主键及索引，并核对 sale:order:* 权限。

-- 2026-10-20 23:54:28 Asia/Shanghai
-- 原因：商品增加基础单位默认售价和私有对象存储主图引用，供销售开单带价与图片展示。
-- 影响对象：base_product.default_sale_price、base_product.image_object_key。
-- 执行顺序：先备份数据库；核实两列均不存在后执行本段；部署 MinIO 并配置私有桶后再发布后端与前端。
-- 注意事项：仅适用于已存在 base_product 且尚无这两列的数据库；执行一次，不得重复执行。
--           本段不迁移旧 image_url，也不修改销售订单历史价格。执行后用 SHOW COLUMNS FROM base_product 校验两列。
ALTER TABLE base_product
    ADD COLUMN default_sale_price DECIMAL(18,4) NULL COMMENT '基础单位默认销售单价',
    ADD COLUMN image_object_key VARCHAR(255) NULL COMMENT '商品主图对象存储键';

-- 2026-10-21 00:15:00 Asia/Shanghai
-- 原因：统一销售开单菜单与静态路由、保存接口所用的 sale:order:create 权限编码。
-- 影响对象：sys_permission 的 sale:order:create、sale:order:entry 记录；旧角色关联迁移见
--           sql/migrate_sale_order_entry_role_permissions_2026_10_21.sql。
-- 执行顺序：已有数据库先备份并手动运行上述一次性迁移脚本，确认 missing_create_grants 和
--           remaining_entry_grants 均为 0，再仅执行本段 UPDATE；不要重跑本文件的历史段落。
--           全新数据库先运行初始化脚本及全部权限种子脚本，再仅执行本段 UPDATE，无需迁移脚本。
-- 前置条件：sale:menu、sale:order:create、sale:order:entry 均已存在且未逻辑删除，
--           sale:order:create 已启用；执行前用 SELECT permission_code,status,deleted
--           FROM sys_permission WHERE permission_code IN
--           ('sale:menu','sale:order:create','sale:order:entry') 核对三条记录。
-- 注意事项：只迁移角色权限，不改用户直接授权或撤销记录；以下 UPDATE 可安全重复执行。
-- 校验：sale:order:create 应为启用的菜单，parent_id 指向 sale:menu，path 为 /sale/new；
--       sale:order:entry 应为停用且 path 为 NULL；/api/v1/menus/tree 不再显示旧节点。
UPDATE sys_permission AS permission
JOIN sys_permission AS menu
  ON menu.permission_code = 'sale:menu' AND menu.deleted = 0
SET permission.permission_name = '销售开单',
    permission.permission_type = 1,
    permission.parent_id = menu.id,
    permission.path = '/sale/new',
    permission.status = 1
WHERE permission.permission_code = 'sale:order:create'
  AND permission.deleted = 0;

UPDATE sys_permission
SET status = 0, path = NULL
WHERE permission_code = 'sale:order:entry'
  AND deleted = 0;

SELECT permission.permission_code, permission.permission_type, permission.parent_id,
       menu.id AS expected_parent_id, permission.path, permission.status
FROM sys_permission AS permission
LEFT JOIN sys_permission AS menu
  ON menu.permission_code = 'sale:menu' AND menu.deleted = 0
WHERE permission.permission_code IN ('sale:order:create', 'sale:order:entry')
  AND permission.deleted = 0;
