# 支付安全约束

> 支付相关代码红线。涉及资金的操作必须经过额外审查。
> 交叉引用: constraints/ai-boundaries.md（总纲支付安全条目）、rules/security.md（安全规范 §4 接口安全）
> 审查流程: workflows/code-review.md（L1 安全检查 + L4 数据库与缓存）

---

## 绝对禁止

```yaml
禁止:
  - 修改支付金额计算逻辑（需 Code Review + 用户确认）
  - 余额扣减无事务保护
  - 余额扣减前不校验余额 >= 扣减金额
  - 支付回调不做签名验证
  - 支付回调不做幂等（必须 Redis setIfAbsent）
  - 退款不做退款单号唯一性校验
  - 在支付流程中使用不精确的浮点数（必须 BigDecimal）
  - 跨模块直接操作支付表（其他模块必须通过 Service/MQ 通信）
  - 支付状态变更绕过 Entity 领域方法（必须通过 markSuccess/markRefunded 等）
```

---

## 必须遵守

```yaml
所有支付代码:
  - 金额字段使用 BigDecimal（Entity/DTO/VO/MQ 消息体均不例外）
  - 支付状态变更通过 Entity 领域方法（markSuccess/markFailed/markRefunded）
  - 支付单归属校验通过 Entity.verifyOwnership(userId)
  - 支付状态变更记录日志
  - 回调接口做幂等防重（Redis setIfAbsent + DB 状态校验双重保护）
  - 退款创建独立退款流水号
  - 新增支付方式必须实现 PayTypeHandler 接口（策略模式）

事务约束:
  - 支付创建 @Transactional(rollbackFor = Exception.class)
  - 支付回调 @Transactional(rollbackFor = Exception.class)
  - 退款发起 @Transactional(rollbackFor = Exception.class)
  - 余额扣减使用 SQL CAS（WHERE balance >= amount），禁止先查后扣

MQ 约束:
  - 支付成功发送 payment-callback topic
  - 退款发起发送 payment-refund topic
  - 消费者必须实现 Redis 幂等（mq:consumed:{topic}:{bizId}）
  - 消费失败删除幂等键并重抛异常（触发 MQ 重试）

Redis Key 约束:
  - 回调幂等: payment:callback:{paymentNo}，TTL 5 分钟
  - 消费幂等: mq:consumed:{topic}:{bizId}，TTL 10 分钟
```

---

## 支付模块修改规则

```yaml
修改 yuemo-payment 模块时:
  必须:
    - 所有涉及金额的操作使用 BigDecimal
    - 所有支付状态变更记录日志
    - 回调接口做幂等防重
    - 退款创建独立退款流水号

  必须审查:
    - 余额扣减逻辑（检查并发安全）
    - 支付回调逻辑（检查幂等和验签）
    - 退款逻辑（检查状态校验和库存恢复）

  必须确认:
    - 任何涉及资金流转的代码变更 → 用户确认
    - 支付接口签名算法的修改 → 用户确认
    - 退款条件的修改 → 用户确认
```

---

## 当前项目支付架构

```yaml
架构模式:
  策略模式: PayTypeHandler 接口 + 多实现
    - BalancePayHandler — 余额支付（已实现）
    - WechatPayHandler — 微信支付（stub，未对接）
    - AlipayPayHandler — 支付宝支付（stub，未对接）
  新增支付方式: 实现 PayTypeHandler 接口 + @Component 注册

支付方式:
  1-微信（stub，未对接）
  2-支付宝（stub，未对接）
  3-平台余额（已实现）

支付流程（余额支付）:
  1. createPayment: @Transactional(校验订单状态 → 幂等检查 → 创建支付单 → PayTypeHandler.doPay → insert)
  2. BalancePayHandler.doPay: 校验余额 → SQL CAS 扣减余额 → 标记成功 → 发送 MQ payment-callback
  3. PaymentCallbackConsumer: 幂等检查 → orderService.paySuccess → 更新订单状态

支付流程（第三方支付）:
  1. createPayment: @Transactional(校验订单状态 → 幂等检查 → 创建支付单 → PayTypeHandler.doPay → insert)
  2. WechatPayHandler/AlipayPayHandler.doPay: 设置 PENDING 状态（预下单 stub）
  3. handleCallback: @Transactional(签名验证 → Redis 幂等 → 状态校验 → markSuccess → 发送 MQ)
  4. PaymentCallbackConsumer: 幂等检查 → orderService.paySuccess → 更新订单状态

并发保护:
  余额扣减: SQL CAS — UPDATE yu_user SET balance = balance - #{amount} WHERE balance >= #{amount}
  支付创建幂等: DB 查询同一 orderId 的 PENDING 支付单
  支付回调幂等: Redis setIfAbsent(payment:callback:{paymentNo}) + DB 状态校验(isPending)

退款流程:
  1. PaymentServiceImpl.refund: @Transactional(校验归属 → markRefunded → 更新支付单 → 发送 MQ payment-refund)
  2. PaymentRefundConsumer: 幂等检查 → userService.addBalance（余额恢复）
  3. OrderRefundConsumer: 幂等检查 → orderService.refundOrder → 更新订单状态 + 释放库存

MQ Topic:
  payment-callback: 支付成功通知（消息体: orderNo String）→ PaymentCallbackConsumer（order 模块）
  payment-refund: 退款通知（消息体: RefundMessage{orderId, userId, amount}）→ PaymentRefundConsumer（payment 模块）+ OrderRefundConsumer（order 模块）

Redis Key:
  payment:callback:{paymentNo} — 回调幂等，TTL 5 分钟
  mq:consumed:payment-callback:{orderNo} — 消费幂等，TTL 10 分钟
  mq:consumed:payment-refund:{orderId} — 退款消费幂等，TTL 10 分钟
  mq:consumed:order-refund:{orderId} — 订单退款消费幂等，TTL 10 分钟
```

---

## AI Agent 强制行为

```yaml
修改支付模块代码时 AI 必须自问:
  1. 是否涉及金额计算？→ 是 → 确认使用 BigDecimal，禁止 float/double
  2. 是否涉及余额扣减？→ 是 → 确认事务保护 + SQL CAS 并发安全
  3. 是否涉及支付回调？→ 是 → 确认签名验证 + 幂等防重
  4. 是否涉及退款？→ 是 → 确认状态校验 + 退款单号唯一 + 幂等
  5. 是否新增支付方式？→ 是 → 必须实现 PayTypeHandler 接口
  6. 是否修改支付状态？→ 是 → 必须通过 Entity 领域方法，禁止直接 setStatus
  7. 是否涉及资金流转？→ 是 → 必须用户确认
```

---

## 已知风险/技术债务

```yaml
⚠️ 高风险:
  - 签名验证为桩实现: verifySign() 始终返回 true，未接入微信/支付宝 SDK 真实验签
    位置: PaymentServiceImpl.handleCallback()
    影响: 任何伪造的回调请求都会被接受
    修复优先级: 接入第三方支付 SDK 前必须修复

  - createPayment 无分布式锁: 高并发下同一订单可能创建重复支付单
    位置: PaymentServiceImpl.createPayment()
    影响: DB 无唯一索引 (order_id, status) 保护，仅靠查询幂等存在竞态
    修复建议: 添加 Redis 分布式锁或 DB 唯一索引

  - 退款接口无幂等保护: 同一订单可重复发起退款
    位置: PaymentServiceImpl.refund()
    影响: 重复退款导致资金损失
    修复建议: 添加 Redis 幂等键或 DB 唯一约束

⚠️ 中风险:
  - 退款单号生成碰撞风险: userId%10000 + IdGenerator.nextId()%1000 组合在高并发下可能碰撞
    位置: PaymentServiceImpl.generateRefundNo()
    修复建议: 使用雪花算法完整 ID 或 DB 唯一索引

  - BalancePayHandler 在 insert 之前发送 MQ: 消费者可能读到未持久化的支付单
    位置: BalancePayHandler.doPay()
    修复建议: 将 MQ 发送移至 insert 之后，或使用 RocketMQ 事务消息

  - OrderRefundConsumer 消息类型不一致: 声明为 String 但生产者发送 RefundMessage
    位置: OrderRefundConsumer
    修复建议: 统一消息类型为 RefundMessage
```
