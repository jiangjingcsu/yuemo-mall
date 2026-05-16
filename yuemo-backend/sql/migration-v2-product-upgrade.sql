-- ============================================
-- 月魔商城 — 商品服务升级迁移 v2
-- 新增：品牌、SKU/规格、标签、搜索词、评价
-- ============================================

USE `yuemo_mall`;

-- ==================== 1. 现有表变更 ====================

ALTER TABLE `yu_product` ADD COLUMN `brand_id` BIGINT DEFAULT NULL COMMENT '品牌ID' AFTER `category_id`;
ALTER TABLE `yu_product` ADD KEY `idx_brand` (`brand_id`);
ALTER TABLE `yu_product` ADD FULLTEXT INDEX `ft_name_title_desc` (`name`, `title`, `description`);

-- ==================== 2. 新表创建 ====================

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

CREATE TABLE IF NOT EXISTS `yu_product_tag_rel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `tag_id` BIGINT NOT NULL COMMENT '标签ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_tag` (`product_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品标签关联表';

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

-- ==================== 3. 种子数据 ====================

INSERT IGNORE INTO `yu_brand` (`id`, `name`, `status`, `sort_order`) VALUES
(1, '苹果', 1, 1),
(2, '华为', 1, 2),
(3, '小米', 1, 3),
(4, '三星', 1, 4),
(5, '罗技', 1, 5),
(6, '联想', 1, 6),
(7, '德芙', 1, 7),
(8, '其他', 1, 99);

INSERT IGNORE INTO `yu_product_tag` (`id`, `name`, `color`, `sort_order`) VALUES
(1, '新品', '#52c41a', 1),
(2, '热销', '#f5222d', 2),
(3, '推荐', '#1677ff', 3);

INSERT IGNORE INTO `yu_product_spec_template` (`id`, `category_id`, `name`, `sort_order`) VALUES
(1, 0, '颜色', 1),
(2, 0, '尺寸', 2),
(3, 0, '版本', 3);

INSERT IGNORE INTO `yu_product_spec_value` (`id`, `template_id`, `value`, `sort_order`) VALUES
(1, 1, '黑色', 1),
(2, 1, '白色', 2),
(3, 1, '红色', 3),
(4, 1, '蓝色', 4),
(5, 1, '钛金色', 5),
(6, 2, 'S', 1),
(7, 2, 'M', 2),
(8, 2, 'L', 3),
(9, 2, 'XL', 4),
(10, 3, '标准版', 1),
(11, 3, '高配版', 2),
(12, 3, '套装', 3);

INSERT IGNORE INTO `yu_search_keyword` (`keyword`, `search_count`, `is_hot`, `sort_order`) VALUES
('手机', 520, 1, 1),
('华为', 320, 1, 2),
('苹果', 280, 1, 3),
('小米', 200, 1, 4),
('连衣裙', 180, 1, 5),
('电脑', 150, 1, 6),
('手表', 120, 1, 7),
('水果', 100, 1, 8);

-- ==================== 4. 现有商品回填 ====================

-- 为数码/手机类商品设置品牌
UPDATE `yu_product` SET `brand_id` = 1 WHERE `name` LIKE '%iPhone%' OR `name` LIKE '%MacBook%' OR `name` LIKE '%Apple%';
UPDATE `yu_product` SET `brand_id` = 2 WHERE `name` LIKE '%华为%';
UPDATE `yu_product` SET `brand_id` = 3 WHERE `name` LIKE '%小米%';
UPDATE `yu_product` SET `brand_id` = 5 WHERE `name` LIKE '%罗技%';
UPDATE `yu_product` SET `brand_id` = 7 WHERE `name` LIKE '%德芙%';

-- 为每个现有商品创建默认 SKU（沿用现有价格和库存）
INSERT INTO `yu_product_sku` (`product_id`, `sku_code`, `spec_ids`, `spec_text`, `price`, `stock`, `image`, `status`)
SELECT
    `id` AS `product_id`,
    CONCAT('SKU-', LPAD(`id`, 6, '0')) AS `sku_code`,
    '[]' AS `spec_ids`,
    '默认' AS `spec_text`,
    `price`,
    `stock`,
    `main_image` AS `image`,
    1 AS `status`
FROM `yu_product`
WHERE `id` NOT IN (SELECT `product_id` FROM `yu_product_sku`);

-- ==================== 5. 验证查询 ====================

SELECT '迁移完成！' AS message;
SELECT COUNT(*) AS brand_count FROM `yu_brand`;
SELECT COUNT(*) AS sku_count FROM `yu_product_sku`;
SELECT COUNT(*) AS tag_count FROM `yu_product_tag`;
SELECT COUNT(*) AS search_keyword_count FROM `yu_search_keyword`;
