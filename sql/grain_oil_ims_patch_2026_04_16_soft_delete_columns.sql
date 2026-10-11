-- Patch: align existing development databases with current soft-delete model.
-- MySQL-compatible idempotent script (works without ADD COLUMN IF NOT EXISTS support).

USE grain_oil_ims;

SET @current_schema = DATABASE();

SET @ddl = (
    SELECT CASE
        WHEN NOT EXISTS(
            SELECT 1
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'sys_user_role'
        ) THEN 'SELECT ''sys_user_role table does not exist'''
        WHEN EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'sys_user_role'
              AND COLUMN_NAME = 'deleted'
        ) THEN 'SELECT ''sys_user_role.deleted already exists'''
        ELSE 'ALTER TABLE `sys_user_role` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 AFTER `created_time`'
    END
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT CASE
        WHEN NOT EXISTS(
            SELECT 1
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'inv_stock'
        ) THEN 'SELECT ''inv_stock table does not exist'''
        WHEN EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'inv_stock'
              AND COLUMN_NAME = 'deleted'
        ) THEN 'SELECT ''inv_stock.deleted already exists'''
        ELSE 'ALTER TABLE `inv_stock` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 AFTER `updated_time`'
    END
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT CASE
        WHEN NOT EXISTS(
            SELECT 1
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'inv_batch'
        ) THEN 'SELECT ''inv_batch table does not exist'''
        WHEN EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'inv_batch'
              AND COLUMN_NAME = 'deleted'
        ) THEN 'SELECT ''inv_batch.deleted already exists'''
        ELSE 'ALTER TABLE `inv_batch` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 AFTER `updated_time`'
    END
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT CASE
        WHEN NOT EXISTS(
            SELECT 1
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'sys_role_permission'
        ) THEN 'SELECT ''sys_role_permission table does not exist'''
        WHEN EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'sys_role_permission'
              AND COLUMN_NAME = 'deleted'
        ) THEN 'SELECT ''sys_role_permission.deleted already exists'''
        ELSE 'ALTER TABLE `sys_role_permission` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 AFTER `created_time`'
    END
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT CASE
        WHEN NOT EXISTS(
            SELECT 1
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'sys_user_permission'
        ) THEN 'SELECT ''sys_user_permission table does not exist'''
        WHEN EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'sys_user_permission'
              AND COLUMN_NAME = 'deleted'
        ) THEN 'SELECT ''sys_user_permission.deleted already exists'''
        ELSE 'ALTER TABLE `sys_user_permission` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 AFTER `created_time`'
    END
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
