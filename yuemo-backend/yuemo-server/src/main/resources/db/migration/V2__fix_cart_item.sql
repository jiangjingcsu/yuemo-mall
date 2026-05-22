-- V2: 购物车表结构修复（幂等，兼容新旧 V1 部署）
-- 使用存储过程避免"索引已存在"和"列不存在"报错

-- 1. 安全删除冗余列（仅当列存在时）
DROP PROCEDURE IF EXISTS `_v2_drop_col`;

CREATE PROCEDURE `_v2_drop_col`(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64)
)
BEGIN
    DECLARE col_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO col_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND COLUMN_NAME = p_column;
    IF col_exists > 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` DROP COLUMN `', p_column, '`');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END;

CALL `_v2_drop_col`('yu_cart_item', 'spec_text');
CALL `_v2_drop_col`('yu_cart_item', 'product_name');
CALL `_v2_drop_col`('yu_cart_item', 'product_image');
CALL `_v2_drop_col`('yu_cart_item', 'price');

DROP PROCEDURE IF EXISTS `_v2_drop_col`;

-- 2. 安全添加唯一索引（仅当索引不存在时）
DROP PROCEDURE IF EXISTS `_v2_add_index`;

CREATE PROCEDURE `_v2_add_index`(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_definition VARCHAR(512)
)
BEGIN
    DECLARE idx_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO idx_exists
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND INDEX_NAME = p_index;
    IF idx_exists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END;

CALL `_v2_add_index`('yu_cart_item', 'uk_user_sku',
    'UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`, `deleted`)');

DROP PROCEDURE IF EXISTS `_v2_add_index`;
