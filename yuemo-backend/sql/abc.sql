-- --------------------------------------------------------
-- 主机:                           192.168.1.56
-- 服务器版本:                        8.0.46 - MySQL Community Server - GPL
-- 服务器操作系统:                      Linux
-- HeidiSQL 版本:                  12.17.0.7270
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


-- 导出 yuemo_mall 的数据库结构
CREATE DATABASE IF NOT EXISTS `yuemo_mall` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `yuemo_mall`;

-- 导出  表 yuemo_mall.flyway_schema_history 结构
CREATE TABLE IF NOT EXISTS `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `script` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 正在导出表  yuemo_mall.flyway_schema_history 的数据：~4 rows (大约)
REPLACE INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES
	(1, '1', '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>', NULL, 'root', '2026-05-20 18:28:48', 0, 1),
	(2, '2', 'fix cart item', 'SQL', 'V2__fix_cart_item.sql', 1754068345, 'root', '2026-05-21 09:21:20', 298, 1),
	(3, '3', 'order item add sku', 'SQL', 'V3__order_item_add_sku.sql', 1208973002, 'root', '2026-05-21 19:29:48', 161, 1),
	(4, '4', 'add user role', 'SQL', 'V4__add_user_role.sql', 896713909, 'root', '2026-05-22 01:56:46', 77, 1);

-- 导出  表 yuemo_mall.yu_address 结构
CREATE TABLE IF NOT EXISTS `yu_address` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `receiver_name` varchar(32) NOT NULL COMMENT '收货人',
  `receiver_phone` varchar(20) NOT NULL COMMENT '联系电话',
  `province` varchar(32) DEFAULT NULL COMMENT '省',
  `city` varchar(32) DEFAULT NULL COMMENT '市',
  `district` varchar(32) DEFAULT NULL COMMENT '区',
  `detail` varchar(256) NOT NULL COMMENT '详细地址',
  `zip_code` varchar(10) DEFAULT NULL COMMENT '邮编',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2057542028492914691 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户地址表';

-- 正在导出表  yuemo_mall.yu_address 的数据：~6 rows (大约)
REPLACE INTO `yu_address` (`id`, `user_id`, `receiver_name`, `receiver_phone`, `province`, `city`, `district`, `detail`, `zip_code`, `is_default`, `create_time`, `update_time`, `deleted`) VALUES
	(1, 1, '张三', '13800138001', '北京市', '市辖区', '朝阳区', '建国路88号SOHO现代城1号楼', '100022', 1, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(2, 1, '张三', '13800138001', '上海市', '市辖区', '浦东新区', '浦东大道1000号', '200120', 0, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(3, 2, '李四', '13800138002', '广东省', '深圳市', '南山区', '科技园南区深南大道9996号', '518057', 1, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(4, 3, '王五', '13800138003', '浙江省', '杭州市', '西湖区', '文三路398号', '310012', 1, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(5, 4, '赵六', '13800138004', '江苏省', '南京市', '鼓楼区', '中山北路1号', '210008', 1, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(2057542028492914690, 2055560838701436929, '姜靖', '18565198905', '湖南', '长沙', '宁乡', '未来方舟小区', NULL, 1, '2026-05-22 03:20:21', '2026-05-22 03:20:21', 0);

-- 导出  表 yuemo_mall.yu_brand 结构
CREATE TABLE IF NOT EXISTS `yu_brand` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) NOT NULL COMMENT '品牌名称',
  `logo` varchar(512) DEFAULT NULL COMMENT '品牌Logo图片',
  `description` varchar(512) DEFAULT NULL COMMENT '品牌描述',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='品牌表';

-- 正在导出表  yuemo_mall.yu_brand 的数据：~8 rows (大约)
REPLACE INTO `yu_brand` (`id`, `name`, `logo`, `description`, `sort_order`, `status`, `create_time`, `update_time`, `deleted`) VALUES
	(1, '苹果', NULL, NULL, 1, 1, '2026-05-16 20:17:29', '2026-05-16 20:17:29', 0),
	(2, '华为', NULL, NULL, 2, 1, '2026-05-16 20:17:29', '2026-05-16 20:17:29', 0),
	(3, '小米', NULL, NULL, 3, 1, '2026-05-16 20:17:29', '2026-05-16 20:17:29', 0),
	(4, '三星', NULL, NULL, 4, 1, '2026-05-16 20:17:29', '2026-05-16 20:17:29', 0),
	(5, '罗技', NULL, NULL, 5, 1, '2026-05-16 20:17:29', '2026-05-16 20:17:29', 0),
	(6, '联想', NULL, NULL, 6, 1, '2026-05-16 20:17:29', '2026-05-16 20:17:29', 0),
	(7, '德芙', NULL, NULL, 7, 1, '2026-05-16 20:17:29', '2026-05-16 20:17:29', 0),
	(8, '其他', NULL, NULL, 99, 1, '2026-05-16 20:17:29', '2026-05-16 20:17:29', 0);

-- 导出  表 yuemo_mall.yu_cart_item 结构
CREATE TABLE IF NOT EXISTS `yu_cart_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `sku_id` bigint DEFAULT NULL COMMENT 'SKU ID',
  `quantity` int NOT NULL DEFAULT '1' COMMENT '数量',
  `selected` tinyint NOT NULL DEFAULT '1' COMMENT '是否勾选',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_sku` (`user_id`,`sku_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2057269006427037723 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='购物车表';

-- 正在导出表  yuemo_mall.yu_cart_item 的数据：~1 rows (大约)
REPLACE INTO `yu_cart_item` (`id`, `user_id`, `product_id`, `sku_id`, `quantity`, `selected`, `create_time`, `update_time`, `deleted`) VALUES
	(2057269006427037697, 2055560838701436929, 17, 17, 1, 1, '2026-05-21 09:15:28', '2026-05-22 03:44:44', 1);

-- 导出  表 yuemo_mall.yu_category 结构
CREATE TABLE IF NOT EXISTS `yu_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) NOT NULL COMMENT '分类名称',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父分类ID',
  `level` tinyint NOT NULL DEFAULT '1' COMMENT '分类层级',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品分类表';

-- 正在导出表  yuemo_mall.yu_category 的数据：~13 rows (大约)
REPLACE INTO `yu_category` (`id`, `name`, `parent_id`, `level`, `sort_order`, `create_time`, `update_time`, `deleted`) VALUES
	(1, '数码电子', 0, 1, 1, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(2, '服装鞋包', 0, 1, 2, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(3, '食品生鲜', 0, 1, 3, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(4, '图书音像', 0, 1, 4, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(5, '手机通讯', 1, 2, 1, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(6, '电脑办公', 1, 2, 2, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(7, '智能穿戴', 1, 2, 3, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(8, '男装', 2, 2, 1, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(9, '女装', 2, 2, 2, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(10, '箱包', 2, 2, 3, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(11, '水果', 3, 2, 1, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(12, '零食', 3, 2, 2, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(13, '图书', 4, 2, 1, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0);

-- 导出  表 yuemo_mall.yu_coupon 结构
CREATE TABLE IF NOT EXISTS `yu_coupon` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(128) NOT NULL COMMENT '优惠券名称',
  `type` tinyint NOT NULL COMMENT '类型 1-满减 2-折扣 3-立减',
  `threshold` decimal(10,2) DEFAULT NULL COMMENT '使用门槛',
  `value` decimal(10,2) NOT NULL COMMENT '优惠金额/折扣率',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '发放总量',
  `received_count` int NOT NULL DEFAULT '0' COMMENT '已领取',
  `used_count` int NOT NULL DEFAULT '0' COMMENT '已使用',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0-未开始 1-进行中 2-已结束',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券表';

-- 正在导出表  yuemo_mall.yu_coupon 的数据：~7 rows (大约)
REPLACE INTO `yu_coupon` (`id`, `name`, `type`, `threshold`, `value`, `total_count`, `received_count`, `used_count`, `start_time`, `end_time`, `status`, `create_time`, `update_time`, `deleted`) VALUES
	(1, '新人专享满100减20', 1, 100.00, 20.00, 10000, 3500, 1200, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 1, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(2, '数码专场9折券', 2, 500.00, 0.90, 5000, 2800, 800, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 1, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(3, '水果生鲜满50减10', 1, 50.00, 10.00, 10000, 5000, 2000, '2024-01-01 00:00:00', '2024-06-30 23:59:59', 1, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(4, '新用户首单立减50', 3, 0.00, 50.00, 100000, 8000, 3000, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 1, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(5, '图书专场满100减30', 1, 100.00, 30.00, 5000, 1500, 400, '2024-01-01 00:00:00', '2024-03-31 23:59:59', 2, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(6, '服饰专区满200减50', 1, 200.00, 50.00, 8000, 3200, 1000, '2024-01-01 00:00:00', '2024-06-30 23:59:59', 1, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(7, '限时秒杀无门槛10元券', 3, 0.00, 10.00, 1000, 1000, 600, '2024-01-20 00:00:00', '2024-01-25 23:59:59', 2, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0);

-- 导出  表 yuemo_mall.yu_order 结构
CREATE TABLE IF NOT EXISTS `yu_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
  `pay_amount` decimal(10,2) NOT NULL COMMENT '实付金额',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0-待支付 1-已支付 2-已发货 3-已完成 4-已取消',
  `address_id` bigint DEFAULT NULL COMMENT '收货地址ID',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `logistics_company` varchar(64) DEFAULT NULL COMMENT '物流公司',
  `logistics_no` varchar(64) DEFAULT NULL COMMENT '物流单号',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `delivery_time` datetime DEFAULT NULL COMMENT '发货时间',
  `receive_time` datetime DEFAULT NULL COMMENT '收货时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2057548164436598787 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表';

-- 正在导出表  yuemo_mall.yu_order 的数据：~9 rows (大约)
REPLACE INTO `yu_order` (`id`, `order_no`, `user_id`, `total_amount`, `pay_amount`, `status`, `address_id`, `remark`, `logistics_company`, `logistics_no`, `pay_time`, `delivery_time`, `receive_time`, `create_time`, `update_time`, `deleted`) VALUES
	(1, 'ORD202401150001', 1, 8999.00, 8999.00, 3, 1, '尽快发货', NULL, NULL, '2024-01-15 10:30:00', NULL, NULL, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(2, 'ORD202401160001', 2, 6598.00, 6598.00, 3, 3, '', NULL, NULL, '2024-01-16 14:20:00', NULL, NULL, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(3, 'ORD202401200001', 1, 1689.00, 1589.00, 3, 1, '使用优惠券减100', NULL, NULL, '2024-01-20 09:15:00', NULL, NULL, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(4, 'ORD202401250001', 3, 6999.00, 6999.00, 1, 4, '', NULL, NULL, '2024-01-25 16:45:00', NULL, NULL, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(5, 'ORD202401260001', 1, 23978.00, 22978.00, 4, 1, '期待已久的新电脑', NULL, NULL, NULL, NULL, NULL, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(6, 'ORD202401270001', 2, 898.00, 898.00, 4, 3, '价格太贵了', NULL, NULL, NULL, NULL, NULL, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(7, 'ORD202401280001', 4, 1280.00, 1180.00, 2, 5, '请仔细包装', NULL, NULL, '2024-01-28 11:30:00', NULL, NULL, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(2057542115927375874, '202605220320426929195', 2055560838701436929, 108.00, 108.00, 4, 2057542028492914700, NULL, NULL, NULL, NULL, NULL, NULL, '2026-05-22 03:20:42', '2026-05-22 03:50:55', 0),
	(2057548164436598786, '202605220344446929676', 2055560838701436929, 36.00, 36.00, 4, 2057542028492914700, NULL, NULL, NULL, NULL, NULL, NULL, '2026-05-22 03:44:44', '2026-05-22 04:15:07', 0);

-- 导出  表 yuemo_mall.yu_order_item 结构
CREATE TABLE IF NOT EXISTS `yu_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `sku_id` bigint DEFAULT NULL COMMENT 'SKU ID',
  `product_name` varchar(128) NOT NULL COMMENT '商品名称（快照）',
  `product_image` varchar(512) DEFAULT NULL COMMENT '商品图片（快照）',
  `spec_text` varchar(128) DEFAULT NULL COMMENT '规格文本（快照）',
  `price` decimal(10,2) NOT NULL COMMENT '购买时单价',
  `quantity` int NOT NULL COMMENT '购买数量',
  `total_amount` decimal(10,2) NOT NULL COMMENT '小计金额',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2057548164767948802 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单明细表';

-- 正在导出表  yuemo_mall.yu_order_item 的数据：~15 rows (大约)
REPLACE INTO `yu_order_item` (`id`, `order_id`, `product_id`, `sku_id`, `product_name`, `product_image`, `spec_text`, `price`, `quantity`, `total_amount`, `create_time`, `update_time`, `deleted`) VALUES
	(1, 1, 1, NULL, 'iPhone 15 Pro', 'https://picsum.photos/seed/iphone15/400/400', NULL, 8999.00, 1, 8999.00, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(2, 2, 7, NULL, 'Apple Watch S9', 'https://picsum.photos/seed/watch9/400/400', NULL, 3299.00, 1, 3299.00, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(3, 2, 11, NULL, '连衣裙', 'https://picsum.photos/seed/dress/400/400', NULL, 299.00, 2, 598.00, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(4, 3, 13, NULL, '阳光玫瑰葡萄', 'https://picsum.photos/seed/grape/400/400', NULL, 89.00, 2, 178.00, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(5, 3, 15, NULL, '坚果礼盒', 'https://picsum.photos/seed/nuts/400/400', NULL, 128.00, 5, 640.00, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(6, 3, 17, NULL, '活着', 'https://picsum.photos/seed/book1/400/400', NULL, 36.00, 1, 36.00, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(7, 4, 2, NULL, '小米14 Ultra', 'https://picsum.photos/seed/xiaomi14/400/400', NULL, 6999.00, 1, 6999.00, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(8, 5, 4, NULL, 'MacBook Pro 14英寸', 'https://picsum.photos/seed/macbook14/400/400', NULL, 14999.00, 1, 14999.00, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(9, 5, 8, NULL, '小米手环8 Pro', 'https://picsum.photos/seed/miband8/400/400', NULL, 399.00, 2, 798.00, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(10, 6, 9, NULL, '纯棉商务衬衫', 'https://picsum.photos/seed/shirt/400/400', NULL, 199.00, 2, 398.00, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(11, 6, 10, NULL, '牛仔裤', 'https://picsum.photos/seed/jeans/400/400', NULL, 259.00, 1, 259.00, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(12, 7, 15, NULL, '坚果礼盒', 'https://picsum.photos/seed/nuts/400/400', NULL, 128.00, 5, 640.00, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(13, 7, 16, NULL, '巧克力礼盒', 'https://picsum.photos/seed/chocolate/400/400', NULL, 98.00, 2, 196.00, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(2057542115944153089, 2057542115927375874, 17, NULL, '活着', 'https://picsum.photos/seed/book1/400/400', NULL, 36.00, 3, 108.00, '2026-05-22 03:20:42', '2026-05-22 03:20:42', 0),
	(2057548164767948801, 2057548164436598786, 17, 17, '活着', 'https://picsum.photos/seed/book1/400/400', '默认', 36.00, 1, 36.00, '2026-05-22 03:44:44', '2026-05-22 03:44:44', 0);

-- 导出  表 yuemo_mall.yu_order_log 结构
CREATE TABLE IF NOT EXISTS `yu_order_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `from_status` tinyint DEFAULT NULL,
  `to_status` tinyint NOT NULL,
  `operator` varchar(64) DEFAULT NULL,
  `remark` varchar(256) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2057549719764213763 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单操作日志';

-- 正在导出表  yuemo_mall.yu_order_log 的数据：~2 rows (大约)
REPLACE INTO `yu_order_log` (`id`, `order_id`, `from_status`, `to_status`, `operator`, `remark`, `create_time`, `update_time`, `deleted`) VALUES
	(2057549719764213761, 2057542115927375874, 0, 4, 'system', '超时自动取消', '2026-05-22 03:50:55', '2026-05-22 03:50:55', 0),
	(2057549719764213762, 2057548164436598786, 0, 4, 'system', '超时自动取消', '2026-05-22 04:15:07', '2026-05-22 04:15:07', 0);

-- 导出  表 yuemo_mall.yu_payment 结构
CREATE TABLE IF NOT EXISTS `yu_payment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `payment_no` varchar(32) DEFAULT NULL COMMENT '支付流水号',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `order_no` varchar(32) DEFAULT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `amount` decimal(10,2) NOT NULL COMMENT '支付金额',
  `pay_type` tinyint DEFAULT NULL COMMENT '支付方式 1-微信 2-支付宝',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0-待支付 1-成功 2-失败 3-已退款',
  `third_trade_no` varchar(64) DEFAULT NULL COMMENT '第三方交易号',
  `refund_no` varchar(32) DEFAULT NULL COMMENT '退款流水号',
  `refund_reason` varchar(512) DEFAULT NULL COMMENT '退款原因',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `refund_time` datetime DEFAULT NULL COMMENT '退款时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付表';

-- 正在导出表  yuemo_mall.yu_payment 的数据：~5 rows (大约)
REPLACE INTO `yu_payment` (`id`, `payment_no`, `order_id`, `order_no`, `user_id`, `amount`, `pay_type`, `status`, `third_trade_no`, `refund_no`, `refund_reason`, `pay_time`, `refund_time`, `create_time`, `update_time`, `deleted`) VALUES
	(1, 'PAY202401150001', 1, 'ORD202401150001', 1, 8999.00, 1, 1, 'WX202401150001234567890', NULL, NULL, '2024-01-15 10:30:00', NULL, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(2, 'PAY202401160001', 2, 'ORD202401160001', 2, 6598.00, 2, 1, 'ALI202401160001234567890', NULL, NULL, '2024-01-16 14:20:00', NULL, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(3, 'PAY202401200001', 3, 'ORD202401200001', 1, 1589.00, 1, 1, 'WX202401200001234567890', NULL, NULL, '2024-01-20 09:15:00', NULL, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(4, 'PAY202401250001', 4, 'ORD202401250001', 3, 6999.00, 2, 1, 'ALI202401250001234567890', NULL, NULL, '2024-01-25 16:45:00', NULL, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(5, 'PAY202401280001', 7, 'ORD202401280001', 4, 1180.00, 1, 1, 'WX202401280001234567890', NULL, NULL, '2024-01-28 11:30:00', NULL, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0);

-- 导出  表 yuemo_mall.yu_product 结构
CREATE TABLE IF NOT EXISTS `yu_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(128) NOT NULL COMMENT '商品名称',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `brand_id` bigint DEFAULT NULL COMMENT '品牌ID',
  `title` varchar(256) DEFAULT NULL COMMENT '商品标题',
  `description` text COMMENT '商品描述',
  `price` decimal(10,2) NOT NULL COMMENT '价格',
  `stock` int NOT NULL DEFAULT '0' COMMENT '库存',
  `main_image` varchar(512) DEFAULT NULL COMMENT '主图片',
  `images` text COMMENT '图片列表（JSON）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0-下架 1-上架',
  `sales` int NOT NULL DEFAULT '0' COMMENT '销量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_name` (`name`),
  KEY `idx_brand` (`brand_id`),
  FULLTEXT KEY `ft_name_title_desc` (`name`,`title`,`description`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品表';

-- 正在导出表  yuemo_mall.yu_product 的数据：~21 rows (大约)
REPLACE INTO `yu_product` (`id`, `name`, `category_id`, `brand_id`, `title`, `description`, `price`, `stock`, `main_image`, `images`, `status`, `sales`, `create_time`, `update_time`, `deleted`) VALUES
	(1, 'iPhone 15 Pro', 5, 1, '苹果 iPhone 15 Pro 256GB 钛金色', 'A17 Pro芯片，钛金属设计，Pro级摄像头系统', 8999.00, 100, 'https://picsum.photos/seed/iphone15/400/400', '["https://picsum.photos/seed/iphone15a/400/400","https://picsum.photos/seed/iphone15b/400/400"]', 1, 520, '2026-05-16 19:22:04', '2026-05-16 20:17:30', 0),
	(2, '小米14 Ultra', 5, 3, '小米14 Ultra 影像旗舰 16+512GB', '徕卡全明星四摄，骁龙8 Gen3处理器', 6999.00, 200, 'https://picsum.photos/seed/xiaomi14/400/400', '["https://picsum.photos/seed/xiaomi14a/400/400"]', 1, 320, '2026-05-16 19:22:04', '2026-05-16 20:17:30', 0),
	(3, '华为Mate 60 Pro', 5, 2, '华为Mate 60 Pro 12+512GB', '麒麟9000S芯片，卫星通话', 7999.00, 150, 'https://picsum.photos/seed/huawei60/400/400', '["https://picsum.photos/seed/huawei60a/400/400"]', 1, 280, '2026-05-16 19:22:04', '2026-05-16 20:17:30', 0),
	(4, 'MacBook Pro 14英寸', 6, 1, 'Apple MacBook Pro 14 M3 Pro芯片', 'M3 Pro芯片，18GB内存，512GB固态', 14999.00, 50, 'https://picsum.photos/seed/macbook14/400/400', '["https://picsum.photos/seed/macbook14a/400/400"]', 1, 150, '2026-05-16 19:22:04', '2026-05-16 20:17:30', 0),
	(5, 'ThinkPad X1 Carbon', 6, NULL, 'ThinkPad X1 Carbon Gen11 14英寸', 'Intel i7处理器，32GB内存，1TB固态', 12999.00, 80, 'https://picsum.photos/seed/thinkpad/400/400', '["https://picsum.photos/seed/thinkpada/400/400"]', 1, 120, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(6, '机械键盘 K380', 6, NULL, '罗技 K380 多设备蓝牙键盘', '轻薄便携，支持三台设备切换', 179.00, 500, 'https://picsum.photos/seed/k380/400/400', '["https://picsum.photos/seed/k380a/400/400"]', 1, 890, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(7, 'Apple Watch S9', 7, 1, 'Apple Watch Series 9 45mm GPS版', 'S9芯片，全天候视网膜显示屏', 3299.00, 120, 'https://picsum.photos/seed/watch9/400/400', '["https://picsum.photos/seed/watch9a/400/400"]', 1, 420, '2026-05-16 19:22:04', '2026-05-16 20:17:30', 0),
	(8, '小米手环8 Pro', 7, 3, '小米手环8 Pro NFC版', '1.74英寸AMOLED大屏，GPS运动追踪', 399.00, 300, 'https://picsum.photos/seed/miband8/400/400', '["https://picsum.photos/seed/miband8a/400/400"]', 1, 680, '2026-05-16 19:22:04', '2026-05-16 20:17:30', 0),
	(9, '纯棉商务衬衫', 8, NULL, '2024新款男士纯棉商务休闲衬衫', '100%纯棉面料，舒适透气，多色可选', 199.00, 800, 'https://picsum.photos/seed/shirt/400/400', '["https://picsum.photos/seed/shirta/400/400"]', 1, 1200, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(10, '牛仔裤', 8, NULL, '男士直筒修身牛仔裤', '优质牛仔布，经典款式，百搭时尚', 259.00, 600, 'https://picsum.photos/seed/jeans/400/400', '["https://picsum.photos/seed/jeansa/400/400"]', 1, 980, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(11, '连衣裙', 9, NULL, '2024新款收腰显瘦碎花连衣裙', '雪纺面料，轻盈飘逸，夏日必备', 299.00, 400, 'https://picsum.photos/seed/dress/400/400', '["https://picsum.photos/seed/dressa/400/400"]', 1, 860, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(12, '针织开衫', 9, NULL, '韩版宽松百搭针织开衫', '柔软针织面料，慵懒风设计', 189.00, 500, 'https://picsum.photos/seed/cardigan/400/400', '["https://picsum.photos/seed/cardigana/400/400"]', 1, 720, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(13, '阳光玫瑰葡萄', 11, NULL, '云南阳光玫瑰葡萄 2斤装', '香甜可口，颗颗饱满，产地直发', 89.00, 1000, 'https://picsum.photos/seed/grape/400/400', '["https://picsum.photos/seed/grapea/400/400"]', 1, 2500, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(14, '智利车厘子', 11, NULL, '智利进口车厘子 JJ级 2斤', '个大饱满，脆甜多汁，新鲜空运', 168.00, 800, 'https://picsum.photos/seed/cherry/400/400', '["https://picsum.photos/seed/cherrya/400/400"]', 1, 1800, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(15, '坚果礼盒', 12, NULL, '每日坚果礼盒 混合坚果 750g', '6种坚果搭配，营养健康，送礼佳品', 128.00, 600, 'https://picsum.photos/seed/nuts/400/400', '["https://picsum.photos/seed/nutsa/400/400"]', 1, 1500, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(16, '巧克力礼盒', 12, NULL, '德芙巧克力礼盒装 520g', '浓郁丝滑，多种口味，浪漫礼盒', 98.00, 700, 'https://picsum.photos/seed/chocolate/400/400', '["https://picsum.photos/seed/chocolatea/400/400"]', 1, 1300, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(17, '活着', 13, NULL, '余华《活着》精装版', '豆瓣9.4高分，经典文学名著', 36.00, 1001, 'https://picsum.photos/seed/book1/400/400', '["https://picsum.photos/seed/book1a/400/400"]', 1, 5000, '2026-05-16 19:22:04', '2026-05-22 17:36:10', 0),
	(18, '三体全套', 13, NULL, '刘慈欣《三体》全套3册', '科幻文学巅峰之作，雨果奖获奖作品', 99.00, 800, 'https://picsum.photos/seed/santi/400/400', '["https://picsum.photos/seed/santia/400/400"]', 1, 3500, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(19, '双肩背包', 10, NULL, '商务休闲双肩背包 15.6英寸', '防泼水面料，多功能收纳，人体工学设计', 299.00, 400, 'https://picsum.photos/seed/backpack/400/400', '["https://picsum.photos/seed/backpacka/400/400"]', 1, 650, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(20, '拉杆箱', 10, NULL, '20寸登机箱 万向轮旅行箱', 'ABS+PC材质，轻便耐用，TSA密码锁', 399.00, 300, 'https://picsum.photos/seed/suitcase/400/400', '["https://picsum.photos/seed/suitcasea/400/400"]', 1, 480, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0),
	(21, '已下架商品', 1, NULL, '这是一款已下架的商品', '库存为0，不再销售', 99.00, 0, 'https://picsum.photos/seed/offline/400/400', '[]', 0, 0, '2026-05-16 19:22:04', '2026-05-16 19:22:04', 0);

-- 导出  表 yuemo_mall.yu_product_review 结构
CREATE TABLE IF NOT EXISTS `yu_product_review` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `sku_id` bigint DEFAULT NULL COMMENT 'SKU ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
  `rating` tinyint NOT NULL COMMENT '评分 1-5',
  `content` text COMMENT '评价内容',
  `images` text COMMENT '评价图片，JSON数组',
  `reply` text COMMENT '商家回复',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '0-隐藏 1-显示',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_rating` (`rating`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品评价表';

-- 正在导出表  yuemo_mall.yu_product_review 的数据：~0 rows (大约)

-- 导出  表 yuemo_mall.yu_product_sku 结构
CREATE TABLE IF NOT EXISTS `yu_product_sku` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` bigint NOT NULL COMMENT 'SPU ID',
  `sku_code` varchar(64) DEFAULT NULL COMMENT 'SKU编码',
  `spec_ids` varchar(512) NOT NULL COMMENT '规格值ID组合，JSON数组如[1,5]',
  `spec_text` varchar(256) NOT NULL COMMENT '规格值文本描述',
  `price` decimal(10,2) NOT NULL COMMENT 'SKU价格',
  `stock` int NOT NULL DEFAULT '0' COMMENT 'SKU库存',
  `image` varchar(512) DEFAULT NULL COMMENT 'SKU图片',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_sku_code` (`sku_code`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SKU表';

-- 正在导出表  yuemo_mall.yu_product_sku 的数据：~21 rows (大约)
REPLACE INTO `yu_product_sku` (`id`, `product_id`, `sku_code`, `spec_ids`, `spec_text`, `price`, `stock`, `image`, `status`, `create_time`, `update_time`, `deleted`) VALUES
	(1, 1, 'SKU-000001', '[]', '默认', 8999.00, 100, 'https://picsum.photos/seed/iphone15/400/400', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(2, 2, 'SKU-000002', '[]', '默认', 6999.00, 200, 'https://picsum.photos/seed/xiaomi14/400/400', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(3, 3, 'SKU-000003', '[]', '默认', 7999.00, 150, 'https://picsum.photos/seed/huawei60/400/400', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(4, 4, 'SKU-000004', '[]', '默认', 14999.00, 50, 'https://picsum.photos/seed/macbook14/400/400', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(5, 5, 'SKU-000005', '[]', '默认', 12999.00, 80, 'https://picsum.photos/seed/thinkpad/400/400', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(6, 6, 'SKU-000006', '[]', '默认', 179.00, 500, 'https://picsum.photos/seed/k380/400/400', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(7, 7, 'SKU-000007', '[]', '默认', 3299.00, 120, 'https://picsum.photos/seed/watch9/400/400', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(8, 8, 'SKU-000008', '[]', '默认', 399.00, 300, 'https://picsum.photos/seed/miband8/400/400', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(9, 9, 'SKU-000009', '[]', '默认', 199.00, 800, 'https://picsum.photos/seed/shirt/400/400', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(10, 10, 'SKU-000010', '[]', '默认', 259.00, 600, 'https://picsum.photos/seed/jeans/400/400', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(11, 11, 'SKU-000011', '[]', '默认', 299.00, 400, 'https://picsum.photos/seed/dress/400/400', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(12, 12, 'SKU-000012', '[]', '默认', 189.00, 500, 'https://picsum.photos/seed/cardigan/400/400', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(13, 13, 'SKU-000013', '[]', '默认', 89.00, 1000, 'https://picsum.photos/seed/grape/400/400', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(14, 14, 'SKU-000014', '[]', '默认', 168.00, 800, 'https://picsum.photos/seed/cherry/400/400', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(15, 15, 'SKU-000015', '[]', '默认', 128.00, 600, 'https://picsum.photos/seed/nuts/400/400', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(16, 16, 'SKU-000016', '[]', '默认', 98.00, 700, 'https://picsum.photos/seed/chocolate/400/400', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(17, 17, 'SKU-000017', '[]', '默认', 36.00, 1000, 'https://picsum.photos/seed/book1/400/400', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(18, 18, 'SKU-000018', '[]', '默认', 99.00, 800, 'https://picsum.photos/seed/santi/400/400', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(19, 19, 'SKU-000019', '[]', '默认', 299.00, 400, 'https://picsum.photos/seed/backpack/400/400', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(20, 20, 'SKU-000020', '[]', '默认', 399.00, 300, 'https://picsum.photos/seed/suitcase/400/400', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(21, 21, 'SKU-000021', '[]', '默认', 99.00, 0, 'https://picsum.photos/seed/offline/400/400', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0);

-- 导出  表 yuemo_mall.yu_product_spec_template 结构
CREATE TABLE IF NOT EXISTS `yu_product_spec_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` bigint NOT NULL DEFAULT '0' COMMENT '适用分类ID，0表示全局',
  `name` varchar(32) NOT NULL COMMENT '规格名称',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='规格模板表';

-- 正在导出表  yuemo_mall.yu_product_spec_template 的数据：~3 rows (大约)
REPLACE INTO `yu_product_spec_template` (`id`, `category_id`, `name`, `sort_order`, `create_time`, `update_time`, `deleted`) VALUES
	(1, 0, '颜色', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(2, 0, '尺寸', 2, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(3, 0, '版本', 3, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0);

-- 导出  表 yuemo_mall.yu_product_spec_value 结构
CREATE TABLE IF NOT EXISTS `yu_product_spec_value` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_id` bigint NOT NULL COMMENT '规格模板ID',
  `value` varchar(64) NOT NULL COMMENT '规格值',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_template` (`template_id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='规格值表';

-- 正在导出表  yuemo_mall.yu_product_spec_value 的数据：~15 rows (大约)
REPLACE INTO `yu_product_spec_value` (`id`, `template_id`, `value`, `sort_order`, `create_time`, `update_time`, `deleted`) VALUES
	(1, 1, '黑色', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(2, 1, '白色', 2, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(3, 1, '红色', 3, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(4, 1, '蓝色', 4, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(5, 1, '钛金色', 5, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(6, 2, 'S', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(7, 2, 'M', 2, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(8, 2, 'L', 3, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(9, 2, 'XL', 4, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(10, 3, '标准版', 1, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(11, 3, '高配版', 2, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(12, 3, '套装', 3, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(13, 3, 'Pro版', 2, '2026-05-21 06:30:06', '2026-05-21 06:30:06', 0),
	(14, 3, 'Max版', 3, '2026-05-21 06:30:06', '2026-05-21 06:30:06', 0),
	(15, 3, 'Ultra版', 4, '2026-05-21 06:30:06', '2026-05-21 06:30:06', 0);

-- 导出  表 yuemo_mall.yu_product_tag 结构
CREATE TABLE IF NOT EXISTS `yu_product_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) NOT NULL COMMENT '标签名',
  `color` varchar(16) DEFAULT NULL COMMENT '标签颜色',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品标签字典表';

-- 正在导出表  yuemo_mall.yu_product_tag 的数据：~3 rows (大约)
REPLACE INTO `yu_product_tag` (`id`, `name`, `color`, `sort_order`, `create_time`, `update_time`, `deleted`) VALUES
	(1, '新品', '#52c41a', 1, '2026-05-16 20:17:29', '2026-05-16 20:17:29', 0),
	(2, '热销', '#f5222d', 2, '2026-05-16 20:17:29', '2026-05-16 20:17:29', 0),
	(3, '推荐', '#1677ff', 3, '2026-05-16 20:17:29', '2026-05-16 20:17:29', 0);

-- 导出  表 yuemo_mall.yu_product_tag_rel 结构
CREATE TABLE IF NOT EXISTS `yu_product_tag_rel` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_tag` (`product_id`,`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品标签关联表';

-- 正在导出表  yuemo_mall.yu_product_tag_rel 的数据：~0 rows (大约)

-- 导出  表 yuemo_mall.yu_search_keyword 结构
CREATE TABLE IF NOT EXISTS `yu_search_keyword` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `keyword` varchar(128) NOT NULL COMMENT '搜索词',
  `search_count` int NOT NULL DEFAULT '0' COMMENT '搜索次数',
  `is_hot` tinyint NOT NULL DEFAULT '0' COMMENT '是否热门推荐',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_keyword` (`keyword`)
) ENGINE=InnoDB AUTO_INCREMENT=56 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='搜索词统计表';

-- 正在导出表  yuemo_mall.yu_search_keyword 的数据：~22 rows (大约)
REPLACE INTO `yu_search_keyword` (`id`, `keyword`, `search_count`, `is_hot`, `sort_order`, `create_time`, `update_time`, `deleted`) VALUES
	(1, '手机', 521, 1, 1, '2026-05-16 20:17:30', '2026-05-16 20:28:57', 0),
	(2, '华为', 320, 1, 2, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(3, '苹果', 280, 1, 3, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(4, '小米', 200, 1, 4, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(5, '连衣裙', 180, 1, 5, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(6, '电脑', 150, 1, 6, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(7, '手表', 120, 1, 7, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(8, '水果', 100, 1, 8, '2026-05-16 20:17:30', '2026-05-16 20:17:30', 0),
	(9, 's', 12, 0, 0, '2026-05-16 20:28:33', '2026-05-16 20:28:53', 0),
	(10, 'sa', 10, 0, 0, '2026-05-16 20:28:33', '2026-05-16 20:28:52', 0),
	(11, 'san', 5, 0, 0, '2026-05-16 20:28:33', '2026-05-16 20:28:51', 0),
	(28, 'san拿', 1, 0, 0, '2026-05-16 20:28:48', '2026-05-16 20:28:48', 0),
	(38, '手', 1, 0, 0, '2026-05-16 20:28:59', '2026-05-16 20:28:59', 0),
	(39, 'q', 2, 0, 0, '2026-05-16 20:29:02', '2026-05-16 20:29:04', 0),
	(40, 'qu', 2, 0, 0, '2026-05-16 20:29:02', '2026-05-16 20:29:04', 0),
	(41, 'qua', 2, 0, 0, '2026-05-16 20:29:02', '2026-05-16 20:29:03', 0),
	(42, 'quan', 1, 0, 0, '2026-05-16 20:29:02', '2026-05-16 20:29:02', 0),
	(46, '三', 3, 0, 0, '2026-05-16 20:29:18', '2026-05-16 20:29:23', 0),
	(49, '笔记本', 80, 1, 2, '2026-05-21 06:30:06', '2026-05-21 06:30:06', 0),
	(50, '耳机', 60, 1, 3, '2026-05-21 06:30:06', '2026-05-21 06:30:06', 0),
	(51, '平板', 50, 1, 4, '2026-05-21 06:30:06', '2026-05-21 06:30:06', 0),
	(52, '键盘', 40, 1, 5, '2026-05-21 06:30:06', '2026-05-21 06:30:06', 0),
	(53, '鼠标', 35, 1, 6, '2026-05-21 06:30:06', '2026-05-21 06:30:06', 0),
	(54, '显示器', 30, 1, 7, '2026-05-21 06:30:06', '2026-05-21 06:30:06', 0),
	(55, '充电器', 25, 1, 8, '2026-05-21 06:30:06', '2026-05-21 06:30:06', 0);

-- 导出  表 yuemo_mall.yu_user 结构
CREATE TABLE IF NOT EXISTS `yu_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(32) NOT NULL COMMENT '用户名',
  `password` varchar(128) NOT NULL COMMENT '密码（BCrypt）',
  `nickname` varchar(64) DEFAULT NULL COMMENT '昵称',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(512) DEFAULT NULL COMMENT '头像',
  `gender` tinyint DEFAULT NULL COMMENT '性别 0-未知 1-男 2-女',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0-正常 1-禁用',
  `balance` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '账户余额',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=2055560838701436930 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- 正在导出表  yuemo_mall.yu_user 的数据：~6 rows (大约)
REPLACE INTO `yu_user` (`id`, `username`, `password`, `nickname`, `phone`, `email`, `avatar`, `gender`, `status`, `balance`, `create_time`, `update_time`, `deleted`) VALUES
	(1, 'testuser', '$2a$10$ueMGloPJmFmyM0/.uy9quuaQHlZStLc5zAjGYqJvGLppLVRiadjm2', '测试用户', '13800138001', 'test@example.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=user1', 1, 0, 0.00, '2026-05-16 19:22:04', '2026-05-16 19:22:44', 0),
	(2, 'alice', '$2a$10$ueMGloPJmFmyM0/.uy9quuaQHlZStLc5zAjGYqJvGLppLVRiadjm2', '爱丽丝', '13800138002', 'alice@example.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=user2', 2, 0, 0.00, '2026-05-16 19:22:04', '2026-05-16 19:22:43', 0),
	(3, 'bob', '$2a$10$ueMGloPJmFmyM0/.uy9quuaQHlZStLc5zAjGYqJvGLppLVRiadjm2', '鲍勃', '13800138003', 'bob@example.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=user3', 1, 0, 0.00, '2026-05-16 19:22:04', '2026-05-16 19:22:42', 0),
	(4, 'charlie', '$2a$10$ueMGloPJmFmyM0/.uy9quuaQHlZStLc5zAjGYqJvGLppLVRiadjm2', '查理', '13800138004', 'charlie@example.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=user4', 1, 0, 0.00, '2026-05-16 19:22:04', '2026-05-16 19:22:40', 0),
	(5, 'disabled_user', '$2a$10$ueMGloPJmFmyM0/.uy9quuaQHlZStLc5zAjGYqJvGLppLVRiadjm2', '已禁用用户', '13800138005', 'disabled@example.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=user5', 1, 1, 0.00, '2026-05-16 19:22:04', '2026-05-16 19:22:38', 0),
	(2055560838701436929, '18565198905', '$2a$10$ueMGloPJmFmyM0/.uy9quuaQHlZStLc5zAjGYqJvGLppLVRiadjm2', NULL, '18565198905', NULL, NULL, NULL, 0, 0.00, '2026-05-16 16:07:49', '2026-05-16 16:07:49', 0);

-- 导出  表 yuemo_mall.yu_user_coupon 结构
CREATE TABLE IF NOT EXISTS `yu_user_coupon` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `coupon_id` bigint NOT NULL COMMENT '优惠券ID',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0-未使用 1-已使用 2-已过期',
  `order_id` bigint DEFAULT NULL COMMENT '使用的订单ID',
  `used_time` datetime DEFAULT NULL COMMENT '使用时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户优惠券表';

-- 正在导出表  yuemo_mall.yu_user_coupon 的数据：~11 rows (大约)
REPLACE INTO `yu_user_coupon` (`id`, `user_id`, `coupon_id`, `status`, `order_id`, `used_time`, `create_time`, `update_time`, `deleted`) VALUES
	(1, 1, 1, 1, 3, '2024-01-20 09:15:00', '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(2, 1, 4, 1, 1, '2024-01-15 10:30:00', '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(3, 1, 2, 0, NULL, NULL, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(4, 2, 1, 1, 2, '2024-01-16 14:20:00', '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(5, 2, 3, 0, NULL, NULL, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(6, 2, 6, 0, NULL, NULL, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(7, 3, 1, 1, 4, '2024-01-25 16:45:00', '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(8, 3, 4, 1, 4, '2024-01-25 16:45:00', '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(9, 4, 1, 1, 7, '2024-01-28 11:30:00', '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(10, 4, 3, 0, NULL, NULL, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0),
	(11, 4, 6, 0, NULL, NULL, '2026-05-16 19:22:05', '2026-05-16 19:22:05', 0);

-- 导出  表 yuemo_mall.yu_user_role 结构
CREATE TABLE IF NOT EXISTS `yu_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `role` varchar(20) NOT NULL DEFAULT 'USER',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色表';

-- 正在导出表  yuemo_mall.yu_user_role 的数据：~0 rows (大约)

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
