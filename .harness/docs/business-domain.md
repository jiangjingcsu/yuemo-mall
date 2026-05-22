# 商城业务领域模型

> AI Agent 理解业务边界和状态流转的权威参考。

---

## 1. 领域总览

```
yuemo-mall
├── 用户域 (user)     — 注册/登录/个人信息/地址管理/角色
├── 商品域 (product)  — 商品/SKU/分类/品牌/规格/评价/搜索/库存
├── 订单域 (order)    — 订单创建/状态流转/物流
├── 库存域 (product)  — 库存扣减/预占/释放（与商品域同模块 yuemo-product）
├── 支付域 (payment)  — 支付单/回调/退款
├── 营销域 (promotion)— 优惠券发放/领取/使用
└── 购物车域 (cart)   — Redis 为主 + MySQL 异步同步
```

Maven 模块映射:

| 领域 | Maven 模块 | 公共模块 |
|------|-----------|---------|
| 用户域 | yuemo-user | common-security（认证/鉴权） |
| 商品域+库存域 | yuemo-product | common-mybatis（ORM） |
| 订单域 | yuemo-order | |
| 支付域 | yuemo-payment | |
| 营销域 | yuemo-promotion | |
| 购物车域 | yuemo-cart | |
| — | yuemo-gateway | 网关（路由/限流/鉴权） |
| — | yuemo-admin | 后台管理 API |
| — | yuemo-server | Spring Boot 启动入口 |
| — | common-core | 通用工具/ID生成/异常 |

---

## 2. 用户域

### 核心实体
- `yu_user` — 用户名(唯一)、密码(BCrypt)、昵称、手机号、邮箱、余额
- `yu_address` — 收货人、电话、省市区+详情、是否默认
- `yu_user_role` — 用户角色关联（唯一约束 `(user_id, deleted)`），默认角色 USER

### 业务规则
- 密码 BCrypt 加密，禁止明文存储
- 手机号脱敏展示 `138****1234`
- 余额操作必须加事务，扣减前校验余额 >= 扣减金额
- JWT Token：accessToken 30min + refreshToken 7天
- 登出时 Token 加入 Redis 黑名单（`token:blacklist:{token}`，TTL 30min）

### 状态
- `User.status`: 0-正常 / 1-禁用

---

## 3. 商品域（含库存）

### 核心实体
- `yu_product` — 商品基础信息 + 库存 + 状态 + 销量
- `yu_product_sku` — SKU（多规格）、价格、库存、规格文本
- `yu_category` — 分类树（parent_id 自引用）
- `yu_brand` — 品牌
- `yu_product_spec_template` — 规格模板（颜色/尺寸/版本等），按分类适用
- `yu_product_spec_value` — 规格值（黑色/白色/S/M/L 等），关联模板
- `yu_product_review` — 评价（1-5星）
- `yu_product_tag` + `yu_product_tag_rel` — 标签关联
- `yu_search_keyword` — 热门搜索统计

### 业务规则
- 库存扣减：先预占（下单），后实扣（支付），超时释放
- 库存不足抛 `SKU_STOCK_INSUFFICIENT`(2006)
- 商品状态：1-上架 / 0-下架，下架商品不出现在搜索结果
- 评价需关联订单（已购才能评）
- SKU 价格不能 <= 0
- SKU 多规格由 `yu_product_spec_template` + `yu_product_spec_value` 支撑

### 库存状态流转
```
下单 → 预占库存(RMQ事务消息 order-stock-preoccupy) → 支付成功(COMMIT) → 库存实扣
                                                     → 取消/超时(ROLLBACK) → 库存释放(order-stock-release)
```

---

## 4. 订单域

### 核心实体
- `yu_order` — 订单号(唯一)、金额、状态、地址快照
- `yu_order_item` — 商品快照（名称/图片/价格/数量/skuId/specText）
- `yu_order_log` — 状态变更日志

### 订单状态机

```
0-待支付 ──支付──→ 1-已支付 ──发货──→ 2-已发货 ──收货──→ 3-已完成
  │                    │
  └──取消/超时──→ 4-已取消
                     │
               3/4/5──退款──→ 5-已退款
```

### 状态变更触发
| 当前→目标 | 触发方式 | 附加操作 |
|-----------|---------|----------|
| 0→4 | 用户取消 / 30min 超时 | 释放库存(RMQ)、释放优惠券 |
| 0→1 | 支付回调 | 记录 payTime |
| 1→2 | 后台发货 | 填写物流单号、记录 deliveryTime |
| 2→3 | 用户确认收货 | 记录 receiveTime |
| 3/4/5→退款 | 退款流程 | 恢复库存、恢复优惠券（如适用） |
| 3/4/5→删除 | 用户删除 | 逻辑删除(deleted=1) |

### 业务规则
- 只有待支付状态(0)可取消
- 取消/超时 => 释放库存（RocketMQ `order-stock-release`）
- 订单创建使用 RMQ 事务消息保证库存预占一致性（Topic: `order-stock-preoccupy`）
- 订单金额 = SUM(order_item.price × quantity)，与支付金额校验

---

## 5. 支付域

### 核心实体
- `yu_payment` — 支付单号(唯一)、订单关联、支付类型、状态、第三方流水

### 支付类型
- 1-微信支付（stub，未真实对接）
- 2-支付宝（stub，未真实对接）
- 3-平台余额（已实现）

### 支付状态
```
0-待支付 → 1-支付成功 → 3-已退款
              ↓
         2-支付失败
```

### 业务规则
- 余额支付：扣减用户余额 + 创建支付单 + 发送 payment-callback 消息
- 回调幂等：`payment:callback:{paymentNo}` Redis setIfAbsent(5min)
- 退款：创建退款记录 + 发送 payment-refund 消息 + 恢复库存

---

## 6. 营销域

### 核心实体
- `yu_coupon` — 优惠券定义（满减/折扣/立减）、总量、已领/已用
- `yu_user_coupon` — 用户领取记录、使用状态、关联订单

### 优惠券类型
- 1-满减（threshold=100, value=10 → 满100减10）
- 2-折扣（value=0.85 → 8.5折）
- 3-立减（value=5 → 无门槛减5）

### 优惠券状态
- `Coupon.status`: 0-未开始 / 1-进行中 / 2-已结束

### 用户券状态
```
0-未使用 → 1-已使用(关联订单)
            → 2-已过期(超过end_time)
```

### 业务规则
- 领取：检查库存 → Redis 去重(`coupon:receive:{userId}:{couponId}`，TTL 7天) → incr received_count
- 使用：订单支付成功后更新用户券状态为已使用
- 过期：定时任务扫描，每天凌晨 3 点（规划中，当前未实现 @Scheduled，过期状态由查询时根据 end_time 判断）

---

## 7. 购物车域

### 核心实体
- Redis Hash: `cart:{userId}` field=`{skuId}` value=JSON(CartItemRedis)，TTL 7天
- MySQL `yu_cart_item` — 异步落库（通过 RocketMQ `cart-sync`）

### 业务规则
- 以 Redis 为主存储，MySQL 为异步备份
- Redis 命中的直接返回；未命中从 MySQL 加载到 Redis
- 每个用户最多 100 个购物车项
- 加购时实时校验 SKU 状态和库存
- 删除为逻辑删除
- 每天凌晨 3 点清理 7 天前未更新的记录
- 唯一约束：`UNIQUE KEY (user_id, sku_id, deleted)`
- 死信队列：`%DLQ%yuemo-cart-consumer` 由 CartSyncDeadLetterConsumer 处理

---

## 8. 跨域规则

| 规则 | 说明 |
|------|------|
| 模块间调用 | 通过 Service 接口，禁止直接调 Mapper |
| 数据库隔离 | 每个模块只操作自己的表，不跨模块操作数据库 |
| 事务边界 | 事务在单个 Service 方法内，不跨模块 |
| 异步通信 | 模块间最终一致性通过 RocketMQ 保证 |
| ID 生成 | 统一使用 `IdGenerator`（时间戳<<12 + 随机数，简化版雪花，无 workerId，多实例部署存在极低概率冲突） |
| Entity 引用 | 当前存在跨模块 Entity 直接引用（Order→Product/Sku, Cart→Product/Sku, Payment→Order），属于设计债务，微服务拆分时需替换为 DTO |

---

## 9. Redis Key 清单

| Key 模式 | 数据结构 | TTL | 用途 | 所属模块 |
|---------|---------|-----|------|---------|
| `token:user:{userId}` | String | 30min | 用户 Token 映射 | common-security |
| `token:blacklist:{token}` | String | 30min | Token 黑名单（登出） | common-security |
| `user:role:{userId}` | String | 24h | 用户角色缓存 | common-security |
| `cart:{userId}` field=`{skuId}` | Hash | 7天 | 购物车数据 | yuemo-cart |
| `coupon:receive:{userId}:{couponId}` | String | 7天 | 优惠券领取去重 | yuemo-promotion |
| `payment:callback:{paymentNo}` | String | 5min | 支付回调幂等 | yuemo-payment |
| `mq:consumed:payment-callback:{orderNo}` | String | 10min | MQ 消费幂等 | yuemo-order |
| `mq:consumed:order-refund:{orderId}` | String | 10min | MQ 消费幂等 | yuemo-order |
| `mq:consumed:payment-refund:{orderId}` | String | 10min | MQ 消费幂等 | yuemo-payment |
| `mq:consumed:stock-release:{fingerprint}` | String | 10min | MQ 消费幂等 | yuemo-product |
| `mq:consumed:stock-preoccupy:{fingerprint}` | String | 10min | MQ 消费幂等 | yuemo-product |
| `rate:{path}:{userId}` | ZSet | 60s | 用户级限流（滑动窗口） | yuemo-gateway |
| `rate:{path}:{ip}` | ZSet | 60s | IP 级限流（滑动窗口） | yuemo-gateway |

---

## 10. RocketMQ Topic 清单

| Topic | Consumer Group | 发送方 | 消费方 | 消息类型 | 说明 |
|-------|---------------|--------|--------|---------|------|
| `order-stock-preoccupy` | `yuemo-product-consumer` | OrderServiceImpl（事务消息） | StockPreoccupyConsumer | Map<Long, Integer> | 下单时库存预占 |
| `order-stock-release` | `yuemo-product-stock-release-consumer` | OrderServiceImpl | StockReleaseConsumer | Map<Long, Integer> | 取消/超时时库存释放 |
| `payment-callback` | `yuemo-order-consumer` | PaymentServiceImpl / BalancePayHandler | PaymentCallbackConsumer | String (orderNo) | 支付成功回调 |
| `payment-refund` | `yuemo-order-refund-consumer` | PaymentServiceImpl | OrderRefundConsumer | String (JSON) | 退款通知（订单侧） |
| `payment-refund` | `yuemo-payment-consumer` | PaymentServiceImpl | PaymentRefundConsumer | RefundMessage | 退款通知（支付侧） |
| `cart-sync` | `yuemo-cart-consumer` | CartServiceImpl | CartSyncConsumer | String (JSON) | 购物车异步落库 |
| `%DLQ%yuemo-cart-consumer` | `yuemo-cart-consumer-dlq` | 系统死信 | CartSyncDeadLetterConsumer | String | 购物车同步死信处理 |

> 注意：`payment-refund` Topic 有两个消费者组（订单侧 + 支付侧），属于广播消费模式。
