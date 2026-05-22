-- ============================================
-- 月魔商城 — 全量表结构（开发环境用）
-- 适用：profile=all，单数据库 yuemo_mall
-- 同步至 Flyway V1~V4 迁移后的最终状态
-- ============================================

CREATE DATABASE IF NOT EXISTS `yuemo_mall`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `yuemo_mall`;

-- ==================== 用户模块 ====================

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

CREATE TABLE IF NOT EXISTS `yu_user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色表';

-- ==================== 商品模块 ====================

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

-- ==================== 订单模块 ====================

CREATE TABLE IF NOT EXISTS `yu_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待支付 1-已支付 2-已发货 3-已完成 4-已取消 5-已退款',
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

CREATE TABLE IF NOT EXISTS `yu_order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `sku_id` BIGINT DEFAULT NULL COMMENT 'SKU ID',
    `product_name` VARCHAR(128) NOT NULL COMMENT '商品名称（快照）',
    `product_image` VARCHAR(512) DEFAULT NULL COMMENT '商品图片（快照）',
    `spec_text` VARCHAR(128) DEFAULT NULL COMMENT '规格文本（快照）',
    `price` DECIMAL(10,2) NOT NULL COMMENT '购买时单价',
    `quantity` INT NOT NULL COMMENT '购买数量',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '小计金额',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- ==================== 支付模块 ====================

CREATE TABLE IF NOT EXISTS `yu_payment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `payment_no` VARCHAR(32) DEFAULT NULL COMMENT '支付流水号',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(32) DEFAULT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    `pay_type` TINYINT DEFAULT NULL COMMENT '支付方式 1-微信 2-支付宝 3-平台余额',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-待支付 1-支付成功 2-支付失败 3-已退款',
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

-- ==================== 购物车模块 ====================

CREATE TABLE IF NOT EXISTS `yu_cart_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
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

-- ==================== 促销模块 ====================

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
