-- V3: 订单明细表增加 SKU 信息

DROP PROCEDURE IF EXISTS `_v3_add_col`;

CREATE PROCEDURE `_v3_add_col`(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition VARCHAR(512)
)
BEGIN
    DECLARE col_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO col_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND COLUMN_NAME = p_column;
    IF col_exists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END;

CALL `_v3_add_col`('yu_order_item', 'sku_id', 'BIGINT DEFAULT NULL COMMENT ''SKU ID'' AFTER `product_id`');
CALL `_v3_add_col`('yu_order_item', 'spec_text', 'VARCHAR(128) DEFAULT NULL COMMENT ''规格文本（快照）'' AFTER `product_image`');

DROP PROCEDURE IF EXISTS `_v3_add_col`;
