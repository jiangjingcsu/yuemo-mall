-- ============================================
-- v3 订单+支付+用户模块升级 — 物流、退款字段 + 操作日志表 + 用户余额
-- ============================================

USE `yuemo_mall`;

-- 用户表新字段
ALTER TABLE `yu_user` ADD COLUMN `balance` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '账户余额' AFTER `status`;

-- 订单表新字段
ALTER TABLE `yu_order` ADD COLUMN `logistics_company` VARCHAR(64) DEFAULT NULL COMMENT '物流公司' AFTER `remark`;
ALTER TABLE `yu_order` ADD COLUMN `logistics_no` VARCHAR(64) DEFAULT NULL COMMENT '物流单号' AFTER `logistics_company`;
ALTER TABLE `yu_order` ADD COLUMN `delivery_time` DATETIME DEFAULT NULL COMMENT '发货时间' AFTER `pay_time`;
ALTER TABLE `yu_order` ADD COLUMN `receive_time` DATETIME DEFAULT NULL COMMENT '收货时间' AFTER `delivery_time`;

-- 支付表新字段
ALTER TABLE `yu_payment` ADD COLUMN `refund_no` VARCHAR(32) DEFAULT NULL COMMENT '退款流水号' AFTER `third_trade_no`;
ALTER TABLE `yu_payment` ADD COLUMN `refund_reason` VARCHAR(512) DEFAULT NULL COMMENT '退款原因' AFTER `refund_no`;
ALTER TABLE `yu_payment` ADD COLUMN `refund_time` DATETIME DEFAULT NULL COMMENT '退款时间' AFTER `pay_time`;

-- 订单操作日志表
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
