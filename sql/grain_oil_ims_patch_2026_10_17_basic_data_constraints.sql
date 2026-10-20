USE grain_oil_ims;

-- Codes (and sibling category names) remain reserved after logical deletion,
-- consistently with the existing unique code indexes. Resolve any duplicate
-- sibling category names before applying this constraint to an older database.
SET @basic_category_index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'base_product_category'
      AND index_name = 'uk_category_parent_name'
);
SET @basic_category_index_sql = IF(@basic_category_index_exists = 0,
    'ALTER TABLE base_product_category ADD UNIQUE KEY uk_category_parent_name (parent_id, category_name)',
    'SELECT 1');
PREPARE basic_category_index_statement FROM @basic_category_index_sql;
EXECUTE basic_category_index_statement;
DEALLOCATE PREPARE basic_category_index_statement;
