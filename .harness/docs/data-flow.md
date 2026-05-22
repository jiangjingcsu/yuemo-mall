# 核心数据流

> AI Agent 理解系统数据流动方式的权威参考。

---

## 1. 下单流程

```
用户 → CheckoutPage → POST /api/order/create
  │
  ▼
OrderController.create()
  │
  ▼
OrderServiceImpl.createOrder()
  ├─ [1] 校验商品/SKU状态、库存，计算金额
  ├─ [2] INSERT yu_order + yu_order_item             ← DB事务(@Transactional)
  ├─ [3] 发送 RMQ 半消息 order-stock-preoccupy       ← 事务消息
  │      └─ OrderStockTransactionListener
  │           ├─ executeLocalTransaction: 查订单存在 → COMMIT
  │           └─ checkLocalTransaction: 查订单存在 → COMMIT
  ├─ [4] COMMIT 后 → StockPreoccupyConsumer
  │      ├─ Redis setIfAbsent 幂等(mq:consumed:stock-preoccupy:{fingerprint}, TTL 10min)
  │      └─ ProductServiceImpl.updateStock()         ← 实扣库存
  └─ [5] cartService.clearSelected(userId)           ← 清空购物车已选(容错,失败不影响订单)

涉及模块: order → product(库存), cart  |  一致性: RMQ事务消息
Redis: 幂等键 mq:consumed:stock-preoccupy:{fingerprint}(TTL 10min)
```

---

## 2. 支付流程

```
用户 → PaymentPage → POST /api/payment/pay (payType=1微信/2支付宝/3余额)
  │
  ▼
PaymentController.pay()
  │
  ▼
PaymentServiceImpl.createPayment()
  ├─ [1] 查询订单(OrderService) → 校验状态=待支付
  ├─ [2] 幂等检查: 同订单是否已有PENDING支付单
  ├─ [3] 策略路由: handlerMap.get(payType)
  │      ├─ payType=3 余额支付 → BalancePayHandler.doPay()（同步）
  │      │   ├─ 查询用户余额 → 校验余额 >= 金额
  │      │   ├─ UserServiceImpl.deductBalance()        ← 扣减余额
  │      │   ├─ 标记支付状态=SUCCESS
  │      │   └─ send("payment-callback", orderNo)      ← 直接发MQ通知
  │      ├─ payType=1 微信支付 → WechatPayHandler.doPay()（异步）
  │      └─ payType=2 支付宝  → AlipayPayHandler.doPay()（异步）
  └─ [4] PaymentMapper.insert()                        ← 创建支付记录

第三方支付回调:
  PaymentController.wechatCallback / alipayCallback
  └─ PaymentServiceImpl.handleCallback()
       ├─ 验签(当前TODO)
       ├─ Redis setIfAbsent 幂等(payment:callback:{paymentNo}, TTL 5min)
       ├─ payment.markSuccess(thirdTradeNo)
       └─ send("payment-callback", orderNo)

支付回调消费（所有支付类型汇聚）:
  PaymentCallbackConsumer.onMessage(orderNo)
  ├─ Redis setIfAbsent 幂等(mq:consumed:payment-callback:{orderNo}, TTL 10min)
  └─ OrderServiceImpl.paySuccess(orderNo)               ← 更新订单状态=已支付

涉及模块: payment → order, user  |  一致性: DB事务 + MQ异步
Redis:
  - payment:callback:{paymentNo}(TTL 5min) — 第三方回调幂等
  - mq:consumed:payment-callback:{orderNo}(TTL 10min) — 消费幂等
```

---

## 3. 退款流程

```
用户 → POST /api/payment/refund?orderId=&reason=
  │
  ▼
PaymentController.refund()
  │
  ▼
PaymentServiceImpl.refund()
  ├─ [1] 查询支付单 → 校验归属 + 状态=已支付
  ├─ [2] @Transactional
  │      └─ payment.markRefunded(refundNo, reason) + updateById
  └─ [3] send("payment-refund", RefundMessage{orderId, userId, amount})

退款消息消费（恢复余额）:
  PaymentRefundConsumer.onMessage(RefundMessage)
  ├─ Redis setIfAbsent 幂等(mq:consumed:payment-refund:{orderId}, TTL 10min)
  └─ UserServiceImpl.addBalance(userId, amount)       ← 退款到余额

订单退款状态变更（由订单模块监听或业务触发）:
  OrderServiceImpl.refundOrder(orderId)
  ├─ 更新订单状态=已退款
  └─ releaseStock(orderId) → send("order-stock-release", skuStockMap)
       └─ StockReleaseConsumer → ProductServiceImpl.restoreStock()  ← 恢复库存

涉及模块: payment → user(余额), order(状态), product(库存)  |  一致性: DB事务 + MQ
Redis: mq:consumed:payment-refund:{orderId}(TTL 10min) — 退款消费幂等
```

---

## 4. 库存扣减流程（完整链路）

```
下单:
  OrderServiceImpl.createOrder()
    → RMQ事务消息(order-stock-preoccupy) → COMMIT
    → StockPreoccupyConsumer → productService.updateStock()
       ├─ Redis setIfAbsent 幂等(mq:consumed:stock-preoccupy:{fingerprint}, TTL 10min)
       └─ UPDATE yu_product SET stock = stock - #{quantity}
          WHERE id = #{productId} AND stock >= #{quantity}

支付成功:
  (库存已在预占时扣减，支付成功不重复扣库存)

取消/超时:
  OrderServiceImpl.cancelOrder() / OrderTimeoutTask(每30s扫描,超时30min)
    → cancelOrderWithCas(CAS乐观锁: status=UNPAID→CANCELLED)
    → releaseStock(orderId) → send("order-stock-release", skuStockMap)
    → StockReleaseConsumer → productService.restoreStock()
       ├─ Redis setIfAbsent 幂等(mq:consumed:stock-release:{fingerprint}, TTL 10min)
       └─ UPDATE yu_product SET stock = stock + #{quantity} WHERE id = #{productId}

涉及模块: order → product(库存)  |  一致性: MQ异步
Redis:
  - mq:consumed:stock-preoccupy:{fingerprint}(TTL 10min) — 预占幂等
  - mq:consumed:stock-release:{fingerprint}(TTL 10min) — 释放幂等
```

---

## 5. 购物车同步流程

```
【写路径】HTTP请求 → Redis写入 → MQ异步落库
  用户加购: POST /api/cart/add
    ├─ Redis HSET cart:{userId} {skuId} = JSON(CartItemRedis)
    └─ RocketMQ send("cart-sync", CartSyncMessage{action=ADD})

  用户修改数量: PUT /api/cart/sku/{skuId}
    ├─ Redis HSET cart:{userId} {skuId} = JSON(更新后)
    └─ RocketMQ send("cart-sync", CartSyncMessage{action=UPDATE})

  用户删除: DELETE /api/cart/sku/{skuId}
    ├─ Redis HDEL cart:{userId} {skuId}
    └─ RocketMQ send("cart-sync", CartSyncMessage{action=REMOVE})

【读路径】Redis命中 → 直接返回；Redis未命中 → MySQL加载
  用户查看: GET /api/cart/list
    ├─ Redis HGETALL cart:{userId}
    │   ├─ 有数据 → buildCartItemVOs(实时查Product/SKU表) → 返回
    │   └─ 无数据 → MySQL查询 → 写入Redis → buildCartItemVOs → 返回

【落库】
  CartSyncConsumer.onMessage()
    ├─ ADD: INSERT ... ON DUPLICATE KEY UPDATE (原子upsert)
    ├─ UPDATE: LambdaUpdateWrapper 合并更新 quantity + selected
    ├─ REMOVE: LambdaQueryWrapper + delete (逻辑删除)
    ├─ CLEAR_SELECTED: delete WHERE user_id=? AND selected=1
    └─ CLEAR_ALL: delete WHERE user_id=?

【清理】CartCleanupTask 每天凌晨3点
  ├─ MySQL: DELETE WHERE update_time < 7天前
  └─ Redis: DEL cart:* (全量清理)

涉及模块: cart内部  |  一致性: Redis优先 + MQ最终一致
Redis Key: cart:{userId}, TTL: 7天(每次写入刷新) + 定时清理(每天凌晨3点清7天前数据)
```

---

## 6. 认证流程

```
【登录】
  POST /api/user/login {username, password}
    → UserServiceImpl.login()
      ├─ 查用户 → BCrypt校验密码
      ├─ 生成 accessToken (JWT, 30min) + refreshToken (7d)
      ├─ Redis SET token:user:{userId} = accessToken (TTL 30min)
      └─ 返回 { accessToken, refreshToken }

【请求鉴权】
  GatewayAuthFilter
    ├─ 白名单匹配 → 放行
    ├─ 半白名单(GET) → 放行
    ├─ 解析 JWT → 校验签名、过期
    ├─ Redis EXISTS token:blacklist:{token} → 是则拒绝
    ├─ 检查 Redis user:role:{userId} == "ADMIN" (管理接口)
    └─ 设置 request.setAttribute("userId", ...)

【登出】
  POST /api/user/logout
    → Redis SET token:blacklist:{token} = 1 (TTL 30min)

Redis Key:
  token:user:{userId}         — 当前Token (TTL 30min)
  token:blacklist:{token}     — 黑名单 (TTL 随Token自然过期)
  user:role:{userId}          — 角色 (TTL 24h)
```

---

## 7. 限流流程

```
RateLimitFilter
  ├─ 对每个 /api/** 路径
  ├─ Redis Lua脚本 (滑动窗口)
  │   └─ ZREMRANGEBYSCORE + ZCARD + ZADD (Sorted Set)
  │       Key: rate:{api-path}:{userId|ip}  TTL: 60s
  ├─ 超过阈值 → HTTP 429
  ├─ 通过 → 放行
  └─ Redis不可用时降级放行

Redis Key: rate:{api-path}:{userId|ip}  TTL: 60s
```

---

## 8. 优惠券流程

```
【领券】
  用户 → POST /api/promotion/coupon/receive?couponId=
    │
    ▼
  CouponServiceImpl.receiveCoupon(userId, couponId)
    ├─ [1] 查询优惠券 → 校验库存(receivedCount < totalCount)
    ├─ [2] Redis setIfAbsent 防重复领取(coupon:receive:{userId}:{couponId}, TTL 领券活动天数)
    ├─ [3] couponMapper.incrementReceivedCount(couponId)  ← 原子递增防超发(返回0=已领完,回滚Redis key)
    └─ [4] INSERT yu_user_coupon(status=0未使用)

【核销】
  CouponServiceImpl.useCoupon(userCouponId, orderId)
    ├─ 校验状态=未使用
    └─ 更新 status=1已使用, orderId

【订单折扣计算】⚠️ 待实现
  CreateOrderDTO.couponId 已定义但 OrderServiceImpl.createOrder() 未使用
  payAmount 直接等于 totalAmount，无优惠扣减
  待实现: 根据 Coupon.type(1满减/2折扣/3立减) + threshold + value 计算折扣

涉及模块: promotion(独立)  |  一致性: DB事务 + Redis防重
Redis:
  - coupon:receive:{userId}:{couponId}(TTL 活动天数) — 防重复领取
  - couponTemplate(all) — 优惠券列表缓存(@CacheEvict)
  - couponPage — 分页缓存(@CacheEvict)
```

---

## 9. 数据流向总图

```
浏览器
  │
  ▼
Nginx (前端静态 + 反向代理)
  │
  ▼
GatewayAuthFilter (JWT鉴权 + 黑名单 + Admin角色校验)
  │
  ▼
RateLimitFilter (Redis滑动窗口限流, 降级放行)
  │
  ▼
CircuitBreakerFilter (Sentinel熔断)
  │
  ▼
Controller (参数接收、调用Service)
  │
  ▼
Service (业务逻辑、事务管理)
  │
  ├──→ Mapper → MySQL
  ├──→ RedisTemplate → Redis
  └──→ RocketMQTemplate → RocketMQ → Consumer → Service → DB

模块间数据流:
  order ──(事务消息)──→ product(库存预占/释放)
  payment ──(MQ)──→ order(支付回调/退款状态)
  payment ──(MQ)──→ user(余额扣减/恢复)
  order ──(直接调用)──→ cart(清空已选)
  promotion ──(独立,待集成)──→ order(优惠券折扣,⚠️待实现)
```
