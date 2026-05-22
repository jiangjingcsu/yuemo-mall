-- ============================================
-- 月魔商城 — 全量修复迁移脚本
-- 无论数据库当前处于哪个版本，执行此脚本后都能补齐到最新状态
-- 所有操作均为幂等（可重复执行不会报错）
-- ============================================

USE `yuemo_mall`;

-- ============================================
-- 辅助存储过程：安全添加列（如果列不存在才添加）
-- ============================================
DROP PROCEDURE IF EXISTS `_safe_add_column`;

DELIMITER $$
CREATE PROCEDURE `_safe_add_column`(
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
END$$
DELIMITER ;

-- ============================================
-- 辅助存储过程：安全添加索引（如果索引不存在才添加）
-- ============================================
DROP PROCEDURE IF EXISTS `_safe_add_index`;

DELIMITER $$
CREATE PROCEDURE `_safe_add_index`(
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
END$$
DELIMITER ;

-- ============================================
-- 辅助存储过程：安全删除列（如果列存在才删除）
-- ============================================
DROP PROCEDURE IF EXISTS `_safe_drop_column`;

DELIMITER $$
CREATE PROCEDURE `_safe_drop_column`(
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
END$$
DELIMITER ;

-- ============================================
-- 一、用户模块
-- ============================================

-- yu_user 表
CREATE TABLE IF NOT EXISTS `yu_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username` VARCHAR(32) NOT NULL COMMENT '用户名',
    `password` VARCHAR(128) NOT NULL COMMENT '密码（BCrypt）',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `avatar` VARCHAR(512) DEFAULT NULL COMMENT '头像',
    `gender` TINYINT DEFAULT NULL COMMENT '性别 0-未知 1-男 2-女',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-正常 1-禁用',
    `balance` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '账户余额',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CALL `_safe_add_column`('yu_user', 'balance', 'DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT \'账户余额\' AFTER `status`');

-- yu_address 表
CREATE TABLE IF NOT EXISTS `yu_address` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `receiver_name` VARCHAR(32) NOT NULL COMMENT '收货人',
    `receiver_phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
    `province` VARCHAR(32) DEFAULT NULL COMMENT '省',
    `city` VARCHAR(32) DEFAULT NULL COMMENT '市',
    `district` VARCHAR(32) DEFAULT NULL COMMENT '区',
    `detail` VARCHAR(256) NOT NULL COMMENT '详细地址',
    `zip_code` VARCHAR(10) DEFAULT NULL COMMENT '邮编',
    `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户地址表';

-- ============================================
-- 二、商品模块
-- ============================================

-- yu_product 表
CREATE TABLE IF NOT EXISTS `yu_product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` VARCHAR(128) NOT NULL COMMENT '商品名称',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `brand_id` BIGINT DEFAULT NULL COMMENT '品牌ID',
    `title` VARCHAR(256) DEFAULT NULL COMMENT '商品标题',
    `description` TEXT COMMENT '商品描述',
    `price` DECIMAL(10,2) NOT NULL COMMENT '价格',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '库存',
    `main_image` VARCHAR(512) DEFAULT NULL COMMENT '主图片',
    `images` TEXT COMMENT '图片列表（JSON）',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-下架 1-上架',
    `sales` INT NOT NULL DEFAULT 0 COMMENT '销量',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_brand` (`brand_id`),
    KEY `idx_status` (`status`),
    KEY `idx_name` (`name`),
    FULLTEXT INDEX `ft_name_title_desc` (`name`, `title`, `description`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

CALL `_safe_add_column`('yu_product', 'brand_id', 'BIGINT DEFAULT NULL COMMENT \'品牌ID\' AFTER `category_id`');
CALL `_safe_add_index`('yu_product', 'idx_brand', 'INDEX `idx_brand` (`brand_id`)');
CALL `_safe_add_index`('yu_product', 'ft_name_title_desc', 'FULLTEXT INDEX `ft_name_title_desc` (`name`, `title`, `description`)');

-- yu_category 表
CREATE TABLE IF NOT EXISTS `yu_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` VARCHAR(64) NOT NULL COMMENT '分类名称',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID',
    `level` TINYINT NOT NULL DEFAULT 1 COMMENT '分类层级',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- yu_brand 表（V2 新增）
CREATE TABLE IF NOT EXISTS `yu_brand` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` VARCHAR(64) NOT NULL COMMENT '品牌名称',
    `logo` VARCHAR(512) DEFAULT NULL COMMENT '品牌Logo图片',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '品牌描述',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='品牌表';

-- yu_product_spec_template 表（V2 新增）
CREATE TABLE IF NOT EXISTS `yu_product_spec_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `category_id` BIGINT NOT NULL DEFAULT 0 COMMENT '适用分类ID，0表示全局',
    `name` VARCHAR(32) NOT NULL COMMENT '规格名称',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规格模板表';

-- yu_product_spec_value 表（V2 新增）
CREATE TABLE IF NOT EXISTS `yu_product_spec_value` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `template_id` BIGINT NOT NULL COMMENT '规格模板ID',
    `value` VARCHAR(64) NOT NULL COMMENT '规格值',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规格值表';

-- yu_product_sku 表（V2 新增）
CREATE TABLE IF NOT EXISTS `yu_product_sku` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `product_id` BIGINT NOT NULL COMMENT 'SPU ID',
    `sku_code` VARCHAR(64) DEFAULT NULL COMMENT 'SKU编码',
    `spec_ids` VARCHAR(512) NOT NULL COMMENT '规格值ID组合，JSON数组如[1,5]',
    `spec_text` VARCHAR(256) NOT NULL COMMENT '规格值文本描述',
    `price` DECIMAL(10,2) NOT NULL COMMENT 'SKU价格',
    `stock` INT NOT NULL DEFAULT 0 COMMENT 'SKU库存',
    `image` VARCHAR(512) DEFAULT NULL COMMENT 'SKU图片',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_product` (`product_id`),
    KEY `idx_sku_code` (`sku_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU表';

-- yu_product_tag 表（V2 新增）
CREATE TABLE IF NOT EXISTS `yu_product_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` VARCHAR(32) NOT NULL COMMENT '标签名',
    `color` VARCHAR(16) DEFAULT NULL COMMENT '标签颜色',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品标签字典表';

-- yu_product_tag_rel 表（V2 新增）
CREATE TABLE IF NOT EXISTS `yu_product_tag_rel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `tag_id` BIGINT NOT NULL COMMENT '标签ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_tag` (`product_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品标签关联表';

-- yu_search_keyword 表（V2 新增）
CREATE TABLE IF NOT EXISTS `yu_search_keyword` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `keyword` VARCHAR(128) NOT NULL COMMENT '搜索词',
    `search_count` INT NOT NULL DEFAULT 0 COMMENT '搜索次数',
    `is_hot` TINYINT NOT NULL DEFAULT 0 COMMENT '是否热门推荐',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_keyword` (`keyword`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索词统计表';

-- yu_product_review 表（V2 新增）
CREATE TABLE IF NOT EXISTS `yu_product_review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `sku_id` BIGINT DEFAULT NULL COMMENT 'SKU ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `order_id` BIGINT DEFAULT NULL COMMENT '关联订单ID',
    `rating` TINYINT NOT NULL COMMENT '评分 1-5',
    `content` TEXT COMMENT '评价内容',
    `images` TEXT COMMENT '评价图片，JSON数组',
    `reply` TEXT COMMENT '商家回复',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0-隐藏 1-显示',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_product` (`product_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_rating` (`rating`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价表';

-- ============================================
-- 三、订单模块
-- ============================================

-- yu_order 表
CREATE TABLE IF NOT EXISTS `yu_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待支付 1-已支付 2-已发货 3-已完成 4-已取消',
    `address_id` BIGINT DEFAULT NULL COMMENT '收货地址ID',
    `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
    `logistics_company` VARCHAR(64) DEFAULT NULL COMMENT '物流公司',
    `logistics_no` VARCHAR(64) DEFAULT NULL COMMENT '物流单号',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `delivery_time` DATETIME DEFAULT NULL COMMENT '发货时间',
    `receive_time` DATETIME DEFAULT NULL COMMENT '收货时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CALL `_safe_add_column`('yu_order', 'logistics_company', 'VARCHAR(64) DEFAULT NULL COMMENT \'物流公司\' AFTER `remark`');
CALL `_safe_add_column`('yu_order', 'logistics_no', 'VARCHAR(64) DEFAULT NULL COMMENT \'物流单号\' AFTER `logistics_company`');
CALL `_safe_add_column`('yu_order', 'delivery_time', 'DATETIME DEFAULT NULL COMMENT \'发货时间\' AFTER `pay_time`');
CALL `_safe_add_column`('yu_order', 'receive_time', 'DATETIME DEFAULT NULL COMMENT \'收货时间\' AFTER `delivery_time`');

-- yu_order_log 表（V3 新增）
CREATE TABLE IF NOT EXISTS `yu_order_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `from_status` TINYINT DEFAULT NULL COMMENT '原状态',
    `to_status` TINYINT NOT NULL COMMENT '新状态',
    `operator` VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单操作日志';

-- yu_order_item 表
CREATE TABLE IF NOT EXISTS `yu_order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(128) NOT NULL COMMENT '商品名称（快照）',
    `product_image` VARCHAR(512) DEFAULT NULL COMMENT '商品图片（快照）',
    `price` DECIMAL(10,2) NOT NULL COMMENT '购买时单价',
    `quantity` INT NOT NULL COMMENT '购买数量',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '小计金额',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- ============================================
-- 四、支付模块
-- ============================================

-- yu_payment 表
CREATE TABLE IF NOT EXISTS `yu_payment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `payment_no` VARCHAR(32) DEFAULT NULL COMMENT '支付流水号',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(32) DEFAULT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    `pay_type` TINYINT DEFAULT NULL COMMENT '支付方式 1-微信 2-支付宝',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-待支付 1-成功 2-失败 3-已退款',
    `third_trade_no` VARCHAR(64) DEFAULT NULL COMMENT '第三方交易号',
    `refund_no` VARCHAR(32) DEFAULT NULL COMMENT '退款流水号',
    `refund_reason` VARCHAR(512) DEFAULT NULL COMMENT '退款原因',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `refund_time` DATETIME DEFAULT NULL COMMENT '退款时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_no` (`payment_no`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付表';

CALL `_safe_add_column`('yu_payment', 'refund_no', 'VARCHAR(32) DEFAULT NULL COMMENT \'退款流水号\' AFTER `third_trade_no`');
CALL `_safe_add_column`('yu_payment', 'refund_reason', 'VARCHAR(512) DEFAULT NULL COMMENT \'退款原因\' AFTER `refund_no`');
CALL `_safe_add_column`('yu_payment', 'refund_time', 'DATETIME DEFAULT NULL COMMENT \'退款时间\' AFTER `pay_time`');

-- ============================================
-- 五、购物车模块
-- ============================================

-- yu_cart_item 表
CREATE TABLE IF NOT EXISTS `yu_cart_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID（冗余，用于快速定位）',
    `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
    `selected` TINYINT NOT NULL DEFAULT 1 COMMENT '是否勾选',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`, `deleted`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

CALL `_safe_drop_column`('yu_cart_item', 'spec_text');
CALL `_safe_drop_column`('yu_cart_item', 'product_name');
CALL `_safe_drop_column`('yu_cart_item', 'product_image');
CALL `_safe_drop_column`('yu_cart_item', 'price');
CALL `_safe_add_index`('yu_cart_item', 'uk_user_sku', 'UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`, `deleted`)');

-- ============================================
-- 六、促销模块
-- ============================================

-- yu_coupon 表
CREATE TABLE IF NOT EXISTS `yu_coupon` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` VARCHAR(128) NOT NULL COMMENT '优惠券名称',
    `type` TINYINT NOT NULL COMMENT '类型 1-满减 2-折扣 3-立减',
    `threshold` DECIMAL(10,2) DEFAULT NULL COMMENT '使用门槛',
    `value` DECIMAL(10,2) NOT NULL COMMENT '优惠金额/折扣率',
    `total_count` INT NOT NULL DEFAULT 0 COMMENT '发放总量',
    `received_count` INT NOT NULL DEFAULT 0 COMMENT '已领取',
    `used_count` INT NOT NULL DEFAULT 0 COMMENT '已使用',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-未开始 1-进行中 2-已结束',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';

-- yu_user_coupon 表
CREATE TABLE IF NOT EXISTS `yu_user_coupon` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `coupon_id` BIGINT NOT NULL COMMENT '优惠券ID',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-未使用 1-已使用 2-已过期',
    `order_id` BIGINT DEFAULT NULL COMMENT '使用的订单ID',
    `used_time` DATETIME DEFAULT NULL COMMENT '使用时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';

-- ============================================
-- 七、种子数据（仅在表为空时插入）
-- ============================================

-- 品牌种子数据
INSERT INTO `yu_brand` (`name`, `logo`, `description`, `sort_order`, `status`)
SELECT '苹果', NULL, 'Apple Inc.', 1, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_brand` WHERE `name` = '苹果');
INSERT INTO `yu_brand` (`name`, `logo`, `description`, `sort_order`, `status`)
SELECT '华为', NULL, 'Huawei', 2, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_brand` WHERE `name` = '华为');
INSERT INTO `yu_brand` (`name`, `logo`, `description`, `sort_order`, `status`)
SELECT '小米', NULL, 'Xiaomi', 3, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_brand` WHERE `name` = '小米');
INSERT INTO `yu_brand` (`name`, `logo`, `description`, `sort_order`, `status`)
SELECT '三星', NULL, 'Samsung', 4, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_brand` WHERE `name` = '三星');
INSERT INTO `yu_brand` (`name`, `logo`, `description`, `sort_order`, `status`)
SELECT '罗技', NULL, 'Logitech', 5, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_brand` WHERE `name` = '罗技');
INSERT INTO `yu_brand` (`name`, `logo`, `description`, `sort_order`, `status`)
SELECT '联想', NULL, 'Lenovo', 6, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_brand` WHERE `name` = '联想');
INSERT INTO `yu_brand` (`name`, `logo`, `description`, `sort_order`, `status`)
SELECT '德芙', NULL, 'Dove Chocolate', 7, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_brand` WHERE `name` = '德芙');
INSERT INTO `yu_brand` (`name`, `logo`, `description`, `sort_order`, `status`)
SELECT '其他', NULL, '其他品牌', 99, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_brand` WHERE `name` = '其他');

-- 商品标签种子数据
INSERT INTO `yu_product_tag` (`name`, `color`, `sort_order`)
SELECT '新品', '#409EFF', 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_product_tag` WHERE `name` = '新品');
INSERT INTO `yu_product_tag` (`name`, `color`, `sort_order`)
SELECT '热销', '#F56C6C', 2 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_product_tag` WHERE `name` = '热销');
INSERT INTO `yu_product_tag` (`name`, `color`, `sort_order`)
SELECT '推荐', '#E6A23C', 3 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_product_tag` WHERE `name` = '推荐');

-- 规格模板种子数据
INSERT INTO `yu_product_spec_template` (`category_id`, `name`, `sort_order`)
SELECT 0, '颜色', 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_product_spec_template` WHERE `name` = '颜色');
INSERT INTO `yu_product_spec_template` (`category_id`, `name`, `sort_order`)
SELECT 0, '尺寸', 2 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_product_spec_template` WHERE `name` = '尺寸');
INSERT INTO `yu_product_spec_template` (`category_id`, `name`, `sort_order`)
SELECT 0, '版本', 3 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_product_spec_template` WHERE `name` = '版本');

-- 规格值种子数据（颜色）
INSERT INTO `yu_product_spec_value` (`template_id`, `value`, `sort_order`)
SELECT t.id, '黑色', 1 FROM `yu_product_spec_template` t WHERE t.`name` = '颜色' AND NOT EXISTS (SELECT 1 FROM `yu_product_spec_value` v WHERE v.`template_id` = t.id AND v.`value` = '黑色');
INSERT INTO `yu_product_spec_value` (`template_id`, `value`, `sort_order`)
SELECT t.id, '白色', 2 FROM `yu_product_spec_template` t WHERE t.`name` = '颜色' AND NOT EXISTS (SELECT 1 FROM `yu_product_spec_value` v WHERE v.`template_id` = t.id AND v.`value` = '白色');
INSERT INTO `yu_product_spec_value` (`template_id`, `value`, `sort_order`)
SELECT t.id, '蓝色', 3 FROM `yu_product_spec_template` t WHERE t.`name` = '颜色' AND NOT EXISTS (SELECT 1 FROM `yu_product_spec_value` v WHERE v.`template_id` = t.id AND v.`value` = '蓝色');
INSERT INTO `yu_product_spec_value` (`template_id`, `value`, `sort_order`)
SELECT t.id, '红色', 4 FROM `yu_product_spec_template` t WHERE t.`name` = '颜色' AND NOT EXISTS (SELECT 1 FROM `yu_product_spec_value` v WHERE v.`template_id` = t.id AND v.`value` = '红色');

-- 规格值种子数据（尺寸）
INSERT INTO `yu_product_spec_value` (`template_id`, `value`, `sort_order`)
SELECT t.id, 'S', 1 FROM `yu_product_spec_template` t WHERE t.`name` = '尺寸' AND NOT EXISTS (SELECT 1 FROM `yu_product_spec_value` v WHERE v.`template_id` = t.id AND v.`value` = 'S');
INSERT INTO `yu_product_spec_value` (`template_id`, `value`, `sort_order`)
SELECT t.id, 'M', 2 FROM `yu_product_spec_template` t WHERE t.`name` = '尺寸' AND NOT EXISTS (SELECT 1 FROM `yu_product_spec_value` v WHERE v.`template_id` = t.id AND v.`value` = 'M');
INSERT INTO `yu_product_spec_value` (`template_id`, `value`, `sort_order`)
SELECT t.id, 'L', 3 FROM `yu_product_spec_template` t WHERE t.`name` = '尺寸' AND NOT EXISTS (SELECT 1 FROM `yu_product_spec_value` v WHERE v.`template_id` = t.id AND v.`value` = 'L');
INSERT INTO `yu_product_spec_value` (`template_id`, `value`, `sort_order`)
SELECT t.id, 'XL', 4 FROM `yu_product_spec_template` t WHERE t.`name` = '尺寸' AND NOT EXISTS (SELECT 1 FROM `yu_product_spec_value` v WHERE v.`template_id` = t.id AND v.`value` = 'XL');

-- 规格值种子数据（版本）
INSERT INTO `yu_product_spec_value` (`template_id`, `value`, `sort_order`)
SELECT t.id, '标准版', 1 FROM `yu_product_spec_template` t WHERE t.`name` = '版本' AND NOT EXISTS (SELECT 1 FROM `yu_product_spec_value` v WHERE v.`template_id` = t.id AND v.`value` = '标准版');
INSERT INTO `yu_product_spec_value` (`template_id`, `value`, `sort_order`)
SELECT t.id, 'Pro版', 2 FROM `yu_product_spec_template` t WHERE t.`name` = '版本' AND NOT EXISTS (SELECT 1 FROM `yu_product_spec_value` v WHERE v.`template_id` = t.id AND v.`value` = 'Pro版');
INSERT INTO `yu_product_spec_value` (`template_id`, `value`, `sort_order`)
SELECT t.id, 'Max版', 3 FROM `yu_product_spec_template` t WHERE t.`name` = '版本' AND NOT EXISTS (SELECT 1 FROM `yu_product_spec_value` v WHERE v.`template_id` = t.id AND v.`value` = 'Max版');
INSERT INTO `yu_product_spec_value` (`template_id`, `value`, `sort_order`)
SELECT t.id, 'Ultra版', 4 FROM `yu_product_spec_template` t WHERE t.`name` = '版本' AND NOT EXISTS (SELECT 1 FROM `yu_product_spec_value` v WHERE v.`template_id` = t.id AND v.`value` = 'Ultra版');

-- 搜索热词种子数据
INSERT INTO `yu_search_keyword` (`keyword`, `search_count`, `is_hot`, `sort_order`)
SELECT '手机', 100, 1, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_search_keyword` WHERE `keyword` = '手机');
INSERT INTO `yu_search_keyword` (`keyword`, `search_count`, `is_hot`, `sort_order`)
SELECT '笔记本', 80, 1, 2 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_search_keyword` WHERE `keyword` = '笔记本');
INSERT INTO `yu_search_keyword` (`keyword`, `search_count`, `is_hot`, `sort_order`)
SELECT '耳机', 60, 1, 3 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_search_keyword` WHERE `keyword` = '耳机');
INSERT INTO `yu_search_keyword` (`keyword`, `search_count`, `is_hot`, `sort_order`)
SELECT '平板', 50, 1, 4 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_search_keyword` WHERE `keyword` = '平板');
INSERT INTO `yu_search_keyword` (`keyword`, `search_count`, `is_hot`, `sort_order`)
SELECT '键盘', 40, 1, 5 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_search_keyword` WHERE `keyword` = '键盘');
INSERT INTO `yu_search_keyword` (`keyword`, `search_count`, `is_hot`, `sort_order`)
SELECT '鼠标', 35, 1, 6 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_search_keyword` WHERE `keyword` = '鼠标');
INSERT INTO `yu_search_keyword` (`keyword`, `search_count`, `is_hot`, `sort_order`)
SELECT '显示器', 30, 1, 7 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_search_keyword` WHERE `keyword` = '显示器');
INSERT INTO `yu_search_keyword` (`keyword`, `search_count`, `is_hot`, `sort_order`)
SELECT '充电器', 25, 1, 8 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `yu_search_keyword` WHERE `keyword` = '充电器');

-- 为现有商品创建默认SKU（如果商品没有SKU记录）
INSERT INTO `yu_product_sku` (`product_id`, `sku_code`, `spec_ids`, `spec_text`, `price`, `stock`, `status`)
SELECT p.id, CONCAT('SKU-', p.id, '-DEFAULT'), '[]', '默认', p.price, p.stock, 1
FROM `yu_product` p
WHERE p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM `yu_product_sku` s WHERE s.product_id = p.id AND s.deleted = 0);

-- 为现有商品设置brand_id（按名称匹配）
UPDATE `yu_product` SET `brand_id` = (SELECT id FROM `yu_brand` WHERE `name` = '苹果' LIMIT 1) WHERE `name` LIKE '%iPhone%' OR `name` LIKE '%iPad%' OR `name` LIKE '%MacBook%' OR `name` LIKE '%AirPods%';
UPDATE `yu_product` SET `brand_id` = (SELECT id FROM `yu_brand` WHERE `name` = '华为' LIMIT 1) WHERE `name` LIKE '%华为%' OR `name` LIKE '%Mate%';
UPDATE `yu_product` SET `brand_id` = (SELECT id FROM `yu_brand` WHERE `name` = '小米' LIMIT 1) WHERE `name` LIKE '%小米%' OR `name` LIKE '%Redmi%';
UPDATE `yu_product` SET `brand_id` = (SELECT id FROM `yu_brand` WHERE `name` = '三星' LIMIT 1) WHERE `name` LIKE '%三星%' OR `name` LIKE '%Galaxy%';
UPDATE `yu_product` SET `brand_id` = (SELECT id FROM `yu_brand` WHERE `name` = '罗技' LIMIT 1) WHERE `name` LIKE '%罗技%';
UPDATE `yu_product` SET `brand_id` = (SELECT id FROM `yu_brand` WHERE `name` = '联想' LIMIT 1) WHERE `name` LIKE '%联想%';
UPDATE `yu_product` SET `brand_id` = (SELECT id FROM `yu_brand` WHERE `name` = '德芙' LIMIT 1) WHERE `name` LIKE '%德芙%';

-- ============================================
-- 清理辅助存储过程
-- ============================================
DROP PROCEDURE IF EXISTS `_safe_add_column`;
DROP PROCEDURE IF EXISTS `_safe_add_index`;

-- ============================================
-- 完成
-- ============================================
SELECT '✅ 数据库修复迁移完成！所有表和字段已同步到最新状态。' AS result;
