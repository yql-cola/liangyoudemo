-- One-time unified-sales schema and permission update for an existing v1.1.0 database.
-- Preserves existing master data. Legacy sales are outside this release and block this update.
-- Back up first; verify biz_sale_out is empty; do not execute on a database initialized from the current init.sql.
USE grain_oil_ims;
DELIMITER $$
DROP PROCEDURE IF EXISTS liangyou_assert_fresh_sales$$
CREATE PROCEDURE liangyou_assert_fresh_sales()
BEGIN
  IF EXISTS (SELECT 1 FROM biz_sale_out LIMIT 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='legacy sales present; unified sales upgrade requires fresh sales test data';
  END IF;
END$$
DELIMITER ;
CALL liangyou_assert_fresh_sales();
DROP PROCEDURE liangyou_assert_fresh_sales;

ALTER TABLE biz_sale_out
  MODIFY COLUMN customer_id BIGINT NULL COMMENT '客户ID，散客为空',
  ADD COLUMN sale_type VARCHAR(16) NOT NULL DEFAULT 'retail' COMMENT '销售类型：retail零售、wholesale批发',
  ADD COLUMN fulfillment_type VARCHAR(16) NOT NULL DEFAULT 'pickup' COMMENT '交付方式：pickup自提、delivery配送',
  ADD COLUMN order_status VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT '订单状态：草稿、待交付、待配送、配送中、待退回、已完成、已取消或已退回',
  ADD COLUMN customer_name_snapshot VARCHAR(128) NOT NULL DEFAULT '散客' COMMENT '本单客户名称快照',
  ADD COLUMN contact_name_snapshot VARCHAR(64) NULL COMMENT '本单收货人快照',
  ADD COLUMN contact_phone_snapshot VARCHAR(32) NULL COMMENT '本单联系电话快照',
  ADD COLUMN delivery_address_snapshot VARCHAR(255) NULL COMMENT '本单配送地址快照',
  ADD COLUMN delivery_remark VARCHAR(255) NULL COMMENT '本单配送备注',
  ADD COLUMN handler_name VARCHAR(64) NULL COMMENT '本单经手人',
  ADD COLUMN planned_delivery_time DATETIME NULL COMMENT '计划送达时间',
  ADD COLUMN completed_time DATETIME NULL COMMENT '实际完成时间',
  ADD COLUMN recorded_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '录单时间',
  ADD KEY idx_sale_order_state_time(order_status,completed_time);
ALTER TABLE biz_sale_out_item
  ADD COLUMN product_name_snapshot VARCHAR(128) NULL COMMENT '保存时商品名称快照',
  ADD COLUMN spec_snapshot VARCHAR(128) NULL COMMENT '保存时商品规格快照',
  ADD COLUMN unit_name_snapshot VARCHAR(64) NULL COMMENT '保存时业务单位名称快照';

CREATE TABLE sale_order_reservation (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '销售订单库存占用ID',
  sale_out_id BIGINT NOT NULL COMMENT '销售订单ID',
  sale_out_item_id BIGINT NOT NULL COMMENT '销售订单明细ID',
  warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
  product_id BIGINT NOT NULL COMMENT '商品ID',
  base_qty DECIMAL(18,4) NOT NULL COMMENT '占用的基础单位数量',
  status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '占用状态：active有效、released已释放、consumed已出库',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sale_reservation_item(sale_out_item_id),
  KEY idx_sale_reservation_order(sale_out_id,status),
  KEY idx_sale_reservation_stock(warehouse_id,product_id,status)
) ENGINE=InnoDB COMMENT='销售订单库存占用记录';
CREATE TABLE sale_order_event (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '销售订单操作记录ID',
  sale_out_id BIGINT NOT NULL COMMENT '销售订单ID',
  event_type VARCHAR(32) NOT NULL COMMENT '操作类型',
  operator_id BIGINT NULL COMMENT '操作人用户ID',
  detail VARCHAR(255) NULL COMMENT '原因或打印模板版本等操作详情',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作请求时间',
  PRIMARY KEY (id),
  KEY idx_sale_event_order(sale_out_id,created_time)
) ENGINE=InnoDB COMMENT='销售配送与打印操作记录';

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
