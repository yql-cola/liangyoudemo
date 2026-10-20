-- Core v1.1.0 schema changes. Run the complete upgrade via scripts/upgrade-v1.1.0.sh.
-- That entry includes soft-delete alignment, constraints and permission seeds in one fail-fast stream.
USE grain_oil_ims;

DELIMITER $$
DROP PROCEDURE IF EXISTS liangyou_v110_add_column$$
CREATE PROCEDURE liangyou_v110_add_column(IN table_name_arg VARCHAR(64), IN column_name_arg VARCHAR(64), IN ddl_arg TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
                   AND table_name=table_name_arg AND column_name=column_name_arg) THEN
        SET @liangyou_v110_ddl = ddl_arg;
        PREPARE liangyou_v110_stmt FROM @liangyou_v110_ddl;
        EXECUTE liangyou_v110_stmt;
        DEALLOCATE PREPARE liangyou_v110_stmt;
    END IF;
END$$
DELIMITER ;

CALL liangyou_v110_add_column('base_unit','status','ALTER TABLE `base_unit` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT ''状态:1启用,0停用''');
CALL liangyou_v110_add_column('base_unit','remark','ALTER TABLE `base_unit` ADD COLUMN `remark` VARCHAR(255) DEFAULT NULL');
CALL liangyou_v110_add_column('base_unit','updated_by','ALTER TABLE `base_unit` ADD COLUMN `updated_by` BIGINT DEFAULT NULL');
CALL liangyou_v110_add_column('base_unit','updated_time','ALTER TABLE `base_unit` ADD COLUMN `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
CALL liangyou_v110_add_column('base_unit','deleted','ALTER TABLE `base_unit` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0');
CALL liangyou_v110_add_column('base_unit','version','ALTER TABLE `base_unit` ADD COLUMN `version` INT NOT NULL DEFAULT 0');
CALL liangyou_v110_add_column('base_product_unit_convert','status','ALTER TABLE `base_product_unit_convert` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT ''状态:1启用,0停用''');
CALL liangyou_v110_add_column('base_product_unit_convert','remark','ALTER TABLE `base_product_unit_convert` ADD COLUMN `remark` VARCHAR(255) DEFAULT NULL');
CALL liangyou_v110_add_column('base_product_unit_convert','updated_by','ALTER TABLE `base_product_unit_convert` ADD COLUMN `updated_by` BIGINT DEFAULT NULL');
CALL liangyou_v110_add_column('base_product_unit_convert','updated_time','ALTER TABLE `base_product_unit_convert` ADD COLUMN `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
CALL liangyou_v110_add_column('base_product_unit_convert','deleted','ALTER TABLE `base_product_unit_convert` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0');
CALL liangyou_v110_add_column('base_product_unit_convert','version','ALTER TABLE `base_product_unit_convert` ADD COLUMN `version` INT NOT NULL DEFAULT 0');
CALL liangyou_v110_add_column('biz_purchase_in_item','base_qty','ALTER TABLE `biz_purchase_in_item` ADD COLUMN `base_qty` DECIMAL(18,4) NULL COMMENT ''保存时基础单位数量快照'' AFTER qty');
CALL liangyou_v110_add_column('biz_sale_out_item','base_qty','ALTER TABLE `biz_sale_out_item` ADD COLUMN `base_qty` DECIMAL(18,4) NULL COMMENT ''保存时基础单位数量快照'' AFTER qty');

DROP PROCEDURE liangyou_v110_add_column;

-- Previous versions moved item.qty directly into inventory; preserve their actual quantities.
UPDATE biz_purchase_in_item SET base_qty=qty WHERE base_qty IS NULL;
UPDATE biz_sale_out_item SET base_qty=qty WHERE base_qty IS NULL;

