-- ============================================
-- 月魔商城 — 测试数据
-- 用于功能测试和开发调试
-- ============================================

USE `yuemo_mall`;

-- ============================================
-- 1. 用户表测试数据
-- 密码统一为: test123456 (BCrypt加密)
-- ============================================

INSERT INTO `yu_user` (`id`, `username`, `password`, `nickname`, `phone`, `email`, `avatar`, `gender`, `status`) VALUES
(1, 'testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '测试用户', '13800138001', 'test@example.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=user1', 1, 0),
(2, 'alice', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '爱丽丝', '13800138002', 'alice@example.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=user2', 2, 0),
(3, 'bob', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '鲍勃', '13800138003', 'bob@example.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=user3', 1, 0),
(4, 'charlie', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '查理', '13800138004', 'charlie@example.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=user4', 1, 0),
(5, 'disabled_user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '已禁用用户', '13800138005', 'disabled@example.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=user5', 1, 1);

-- ============================================
-- 2. 用户地址表测试数据
-- ============================================

INSERT INTO `yu_address` (`id`, `user_id`, `receiver_name`, `receiver_phone`, `province`, `city`, `district`, `detail`, `zip_code`, `is_default`) VALUES
(1, 1, '张三', '13800138001', '北京市', '市辖区', '朝阳区', '建国路88号SOHO现代城1号楼', '100022', 1),
(2, 1, '张三', '13800138001', '上海市', '市辖区', '浦东新区', '浦东大道1000号', '200120', 0),
(3, 2, '李四', '13800138002', '广东省', '深圳市', '南山区', '科技园南区深南大道9996号', '518057', 1),
(4, 3, '王五', '13800138003', '浙江省', '杭州市', '西湖区', '文三路398号', '310012', 1),
(5, 4, '赵六', '13800138004', '江苏省', '南京市', '鼓楼区', '中山北路1号', '210008', 1);

-- ============================================
-- 3. 商品分类表测试数据
-- ============================================

INSERT INTO `yu_category` (`id`, `name`, `parent_id`, `level`, `sort_order`) VALUES
(1, '数码电子', 0, 1, 1),
(2, '服装鞋包', 0, 1, 2),
(3, '食品生鲜', 0, 1, 3),
(4, '图书音像', 0, 1, 4),
(5, '手机通讯', 1, 2, 1),
(6, '电脑办公', 1, 2, 2),
(7, '智能穿戴', 1, 2, 3),
(8, '男装', 2, 2, 1),
(9, '女装', 2, 2, 2),
(10, '箱包', 2, 2, 3),
(11, '水果', 3, 2, 1),
(12, '零食', 3, 2, 2),
(13, '图书', 4, 2, 1);

-- ============================================
-- 4. 商品表测试数据
-- ============================================

INSERT INTO `yu_product` (`id`, `name`, `category_id`, `title`, `description`, `price`, `stock`, `main_image`, `images`, `status`, `sales`) VALUES
-- 手机通讯
(1, 'iPhone 15 Pro', 5, '苹果 iPhone 15 Pro 256GB 钛金色', 'A17 Pro芯片，钛金属设计，Pro级摄像头系统', 8999.00, 100, 'https://picsum.photos/seed/iphone15/400/400', '["https://picsum.photos/seed/iphone15a/400/400","https://picsum.photos/seed/iphone15b/400/400"]', 1, 520),
(2, '小米14 Ultra', 5, '小米14 Ultra 影像旗舰 16+512GB', '徕卡全明星四摄，骁龙8 Gen3处理器', 6999.00, 200, 'https://picsum.photos/seed/xiaomi14/400/400', '["https://picsum.photos/seed/xiaomi14a/400/400"]', 1, 320),
(3, '华为Mate 60 Pro', 5, '华为Mate 60 Pro 12+512GB', '麒麟9000S芯片，卫星通话', 7999.00, 150, 'https://picsum.photos/seed/huawei60/400/400', '["https://picsum.photos/seed/huawei60a/400/400"]', 1, 280),

-- 电脑办公
(4, 'MacBook Pro 14英寸', 6, 'Apple MacBook Pro 14 M3 Pro芯片', 'M3 Pro芯片，18GB内存，512GB固态', 14999.00, 50, 'https://picsum.photos/seed/macbook14/400/400', '["https://picsum.photos/seed/macbook14a/400/400"]', 1, 150),
(5, 'ThinkPad X1 Carbon', 6, 'ThinkPad X1 Carbon Gen11 14英寸', 'Intel i7处理器，32GB内存，1TB固态', 12999.00, 80, 'https://picsum.photos/seed/thinkpad/400/400', '["https://picsum.photos/seed/thinkpada/400/400"]', 1, 120),
(6, '机械键盘 K380', 6, '罗技 K380 多设备蓝牙键盘', '轻薄便携，支持三台设备切换', 179.00, 500, 'https://picsum.photos/seed/k380/400/400', '["https://picsum.photos/seed/k380a/400/400"]', 1, 890),

-- 智能穿戴
(7, 'Apple Watch S9', 7, 'Apple Watch Series 9 45mm GPS版', 'S9芯片，全天候视网膜显示屏', 3299.00, 120, 'https://picsum.photos/seed/watch9/400/400', '["https://picsum.photos/seed/watch9a/400/400"]', 1, 420),
(8, '小米手环8 Pro', 7, '小米手环8 Pro NFC版', '1.74英寸AMOLED大屏，GPS运动追踪', 399.00, 300, 'https://picsum.photos/seed/miband8/400/400', '["https://picsum.photos/seed/miband8a/400/400"]', 1, 680),

-- 男装
(9, '纯棉商务衬衫', 8, '2024新款男士纯棉商务休闲衬衫', '100%纯棉面料，舒适透气，多色可选', 199.00, 800, 'https://picsum.photos/seed/shirt/400/400', '["https://picsum.photos/seed/shirta/400/400"]', 1, 1200),
(10, '牛仔裤', 8, '男士直筒修身牛仔裤', '优质牛仔布，经典款式，百搭时尚', 259.00, 600, 'https://picsum.photos/seed/jeans/400/400', '["https://picsum.photos/seed/jeansa/400/400"]', 1, 980),

-- 女装
(11, '连衣裙', 9, '2024新款收腰显瘦碎花连衣裙', '雪纺面料，轻盈飘逸，夏日必备', 299.00, 400, 'https://picsum.photos/seed/dress/400/400', '["https://picsum.photos/seed/dressa/400/400"]', 1, 860),
(12, '针织开衫', 9, '韩版宽松百搭针织开衫', '柔软针织面料，慵懒风设计', 189.00, 500, 'https://picsum.photos/seed/cardigan/400/400', '["https://picsum.photos/seed/cardigana/400/400"]', 1, 720),

-- 水果
(13, '阳光玫瑰葡萄', 11, '云南阳光玫瑰葡萄 2斤装', '香甜可口，颗颗饱满，产地直发', 89.00, 1000, 'https://picsum.photos/seed/grape/400/400', '["https://picsum.photos/seed/grapea/400/400"]', 1, 2500),
(14, '智利车厘子', 11, '智利进口车厘子 JJ级 2斤', '个大饱满，脆甜多汁，新鲜空运', 168.00, 800, 'https://picsum.photos/seed/cherry/400/400', '["https://picsum.photos/seed/cherrya/400/400"]', 1, 1800),

-- 零食
(15, '坚果礼盒', 12, '每日坚果礼盒 混合坚果 750g', '6种坚果搭配，营养健康，送礼佳品', 128.00, 600, 'https://picsum.photos/seed/nuts/400/400', '["https://picsum.photos/seed/nutsa/400/400"]', 1, 1500),
(16, '巧克力礼盒', 12, '德芙巧克力礼盒装 520g', '浓郁丝滑，多种口味，浪漫礼盒', 98.00, 700, 'https://picsum.photos/seed/chocolate/400/400', '["https://picsum.photos/seed/chocolatea/400/400"]', 1, 1300),

-- 图书
(17, '活着', 13, '余华《活着》精装版', '豆瓣9.4高分，经典文学名著', 36.00, 1000, 'https://picsum.photos/seed/book1/400/400', '["https://picsum.photos/seed/book1a/400/400"]', 1, 5000),
(18, '三体全套', 13, '刘慈欣《三体》全套3册', '科幻文学巅峰之作，雨果奖获奖作品', 99.00, 800, 'https://picsum.photos/seed/santi/400/400', '["https://picsum.photos/seed/santia/400/400"]', 1, 3500),

-- 箱包
(19, '双肩背包', 10, '商务休闲双肩背包 15.6英寸', '防泼水面料，多功能收纳，人体工学设计', 299.00, 400, 'https://picsum.photos/seed/backpack/400/400', '["https://picsum.photos/seed/backpacka/400/400"]', 1, 650),
(20, '拉杆箱', 10, '20寸登机箱 万向轮旅行箱', 'ABS+PC材质，轻便耐用，TSA密码锁', 399.00, 300, 'https://picsum.photos/seed/suitcase/400/400', '["https://picsum.photos/seed/suitcasea/400/400"]', 1, 480),

-- 下架商品（用于测试）
(21, '已下架商品', 1, '这是一款已下架的商品', '库存为0，不再销售', 99.00, 0, 'https://picsum.photos/seed/offline/400/400', '[]', 0, 0);

-- ============================================
-- 5. 购物车测试数据
-- ============================================

INSERT INTO `yu_cart_item` (`id`, `user_id`, `product_id`, `product_name`, `product_image`, `price`, `quantity`, `selected`) VALUES
(1, 1, 1, 'iPhone 15 Pro', 'https://picsum.photos/seed/iphone15/400/400', 8999.00, 1, 1),
(2, 1, 4, 'MacBook Pro 14英寸', 'https://picsum.photos/seed/macbook14/400/400', 14999.00, 1, 1),
(3, 1, 13, '阳光玫瑰葡萄', 'https://picsum.photos/seed/grape/400/400', 89.00, 2, 1),
(4, 1, 17, '活着', 'https://picsum.photos/seed/book1/400/400', 36.00, 1, 0),
(5, 1, 9, '纯棉商务衬衫', 'https://picsum.photos/seed/shirt/400/400', 199.00, 3, 1),
(6, 2, 7, 'Apple Watch S9', 'https://picsum.photos/seed/watch9/400/400', 3299.00, 1, 1),
(7, 2, 11, '连衣裙', 'https://picsum.photos/seed/dress/400/400', 299.00, 2, 1),
(8, 3, 2, '小米14 Ultra', 'https://picsum.photos/seed/xiaomi14/400/400', 6999.00, 1, 1),
(9, 4, 15, '坚果礼盒', 'https://picsum.photos/seed/nuts/400/400', 128.00, 5, 1);

-- ============================================
-- 6. 订单测试数据
-- ============================================

INSERT INTO `yu_order` (`id`, `order_no`, `user_id`, `total_amount`, `pay_amount`, `status`, `address_id`, `remark`, `pay_time`) VALUES
(1, 'ORD202401150001', 1, 8999.00, 8999.00, 3, 1, '尽快发货', '2024-01-15 10:30:00'),
(2, 'ORD202401160001', 2, 6598.00, 6598.00, 3, 3, '', '2024-01-16 14:20:00'),
(3, 'ORD202401200001', 1, 1689.00, 1589.00, 3, 1, '使用优惠券减100', '2024-01-20 09:15:00'),
(4, 'ORD202401250001', 3, 6999.00, 6999.00, 1, 4, '', '2024-01-25 16:45:00'),
(5, 'ORD202401260001', 1, 23978.00, 22978.00, 0, 1, '期待已久的新电脑', NULL),
(6, 'ORD202401270001', 2, 898.00, 898.00, 4, 3, '价格太贵了', NULL),
(7, 'ORD202401280001', 4, 1280.00, 1180.00, 2, 5, '请仔细包装', '2024-01-28 11:30:00');

-- ============================================
-- 7. 订单明细测试数据
-- ============================================

INSERT INTO `yu_order_item` (`id`, `order_id`, `product_id`, `product_name`, `product_image`, `price`, `quantity`, `total_amount`) VALUES
(1, 1, 1, 'iPhone 15 Pro', 'https://picsum.photos/seed/iphone15/400/400', 8999.00, 1, 8999.00),
(2, 2, 7, 'Apple Watch S9', 'https://picsum.photos/seed/watch9/400/400', 3299.00, 1, 3299.00),
(3, 2, 11, '连衣裙', 'https://picsum.photos/seed/dress/400/400', 299.00, 2, 598.00),
(4, 3, 13, '阳光玫瑰葡萄', 'https://picsum.photos/seed/grape/400/400', 89.00, 2, 178.00),
(5, 3, 15, '坚果礼盒', 'https://picsum.photos/seed/nuts/400/400', 128.00, 5, 640.00),
(6, 3, 17, '活着', 'https://picsum.photos/seed/book1/400/400', 36.00, 1, 36.00),
(7, 4, 2, '小米14 Ultra', 'https://picsum.photos/seed/xiaomi14/400/400', 6999.00, 1, 6999.00),
(8, 5, 4, 'MacBook Pro 14英寸', 'https://picsum.photos/seed/macbook14/400/400', 14999.00, 1, 14999.00),
(9, 5, 8, '小米手环8 Pro', 'https://picsum.photos/seed/miband8/400/400', 399.00, 2, 798.00),
(10, 6, 9, '纯棉商务衬衫', 'https://picsum.photos/seed/shirt/400/400', 199.00, 2, 398.00),
(11, 6, 10, '牛仔裤', 'https://picsum.photos/seed/jeans/400/400', 259.00, 1, 259.00),
(12, 7, 15, '坚果礼盒', 'https://picsum.photos/seed/nuts/400/400', 128.00, 5, 640.00),
(13, 7, 16, '巧克力礼盒', 'https://picsum.photos/seed/chocolate/400/400', 98.00, 2, 196.00);

-- ============================================
-- 8. 支付测试数据
-- ============================================

INSERT INTO `yu_payment` (`id`, `payment_no`, `order_id`, `order_no`, `user_id`, `amount`, `pay_type`, `status`, `third_trade_no`, `pay_time`) VALUES
(1, 'PAY202401150001', 1, 'ORD202401150001', 1, 8999.00, 1, 1, 'WX202401150001234567890', '2024-01-15 10:30:00'),
(2, 'PAY202401160001', 2, 'ORD202401160001', 2, 6598.00, 2, 1, 'ALI202401160001234567890', '2024-01-16 14:20:00'),
(3, 'PAY202401200001', 3, 'ORD202401200001', 1, 1589.00, 1, 1, 'WX202401200001234567890', '2024-01-20 09:15:00'),
(4, 'PAY202401250001', 4, 'ORD202401250001', 3, 6999.00, 2, 1, 'ALI202401250001234567890', '2024-01-25 16:45:00'),
(5, 'PAY202401280001', 7, 'ORD202401280001', 4, 1180.00, 1, 1, 'WX202401280001234567890', '2024-01-28 11:30:00');

-- ============================================
-- 9. 优惠券测试数据
-- ============================================

INSERT INTO `yu_coupon` (`id`, `name`, `type`, `threshold`, `value`, `total_count`, `received_count`, `used_count`, `start_time`, `end_time`, `status`) VALUES
(1, '新人专享满100减20', 1, 100.00, 20.00, 10000, 3500, 1200, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 1),
(2, '数码专场9折券', 2, 500.00, 0.90, 5000, 2800, 800, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 1),
(3, '水果生鲜满50减10', 1, 50.00, 10.00, 10000, 5000, 2000, '2024-01-01 00:00:00', '2024-06-30 23:59:59', 1),
(4, '新用户首单立减50', 3, 0.00, 50.00, 100000, 8000, 3000, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 1),
(5, '图书专场满100减30', 1, 100.00, 30.00, 5000, 1500, 400, '2024-01-01 00:00:00', '2024-03-31 23:59:59', 2),
(6, '服饰专区满200减50', 1, 200.00, 50.00, 8000, 3200, 1000, '2024-01-01 00:00:00', '2024-06-30 23:59:59', 1),
(7, '限时秒杀无门槛10元券', 3, 0.00, 10.00, 1000, 1000, 600, '2024-01-20 00:00:00', '2024-01-25 23:59:59', 2);

-- ============================================
-- 10. 用户优惠券测试数据
-- ============================================

INSERT INTO `yu_user_coupon` (`id`, `user_id`, `coupon_id`, `status`, `order_id`, `used_time`) VALUES
(1, 1, 1, 1, 3, '2024-01-20 09:15:00'),
(2, 1, 4, 1, 1, '2024-01-15 10:30:00'),
(3, 1, 2, 0, NULL, NULL),
(4, 2, 1, 1, 2, '2024-01-16 14:20:00'),
(5, 2, 3, 0, NULL, NULL),
(6, 2, 6, 0, NULL, NULL),
(7, 3, 1, 1, 4, '2024-01-25 16:45:00'),
(8, 3, 4, 1, 4, '2024-01-25 16:45:00'),
(9, 4, 1, 1, 7, '2024-01-28 11:30:00'),
(10, 4, 3, 0, NULL, NULL),
(11, 4, 6, 0, NULL, NULL);

-- ============================================
-- 验证查询
-- ============================================

SELECT '测试数据插入完成！' AS message;

SELECT '用户数据:' AS '';
SELECT COUNT(*) AS user_count FROM yu_user;

SELECT '商品分类:' AS '';
SELECT COUNT(*) AS category_count FROM yu_category;

SELECT '商品数据:' AS '';
SELECT COUNT(*) AS product_count FROM yu_product;
SELECT SUM(stock) AS total_stock FROM yu_product;

SELECT '购物车数据:' AS '';
SELECT COUNT(*) AS cart_count FROM yu_cart_item;

SELECT '订单数据:' AS '';
SELECT COUNT(*) AS order_count FROM yu_order;
SELECT COUNT(*) AS order_item_count FROM yu_order_item;

SELECT '支付数据:' AS '';
SELECT COUNT(*) AS payment_count FROM yu_payment;

SELECT '优惠券数据:' AS '';
SELECT COUNT(*) AS coupon_count FROM yu_coupon;
SELECT COUNT(*) AS user_coupon_count FROM yu_user_coupon;
