# 消息队列治理规则

> 约束 AI Agent 的 RocketMQ 使用方式、Topic 命名、消息设计和消费规范。基于项目实际 MQ 使用模式。
>
> **层级定位**：本文件是 Rules 层文档，定义 RocketMQ 使用方式的治理规范（软规范，违反 = 应修复）。
> 与 Constraints 层的关系：Constraints（constraints/ai-boundaries.md）定义 MQ 操作硬红线（违反 = 停止执行），本文件定义详细规范。
> 实操技能：`.harness/skills/transaction-analysis/SKILL.md`

---

## 1. Topic 命名规范

```
格式: {domain}-{event}

当前项目 Topic:
  order-stock-preoccupy      — 订单库存预占（事务消息）
  order-stock-release        — 订单库存释放
  payment-callback           — 支付成功回调
  payment-refund             — 退款通知
  cart-sync                  — 购物车 Redis→MySQL 同步

Topic 规格详情:
  order-stock-preoccupy:
    生产者: OrderServiceImpl（事务消息）
    消费者: StockPreoccupyConsumer（yuemo-product）
    消息体: Map<Long, Integer>（skuId→quantity）⚠️ 应改为 record
    发送方式: 事务消息（OrderStockTransactionListener）
    幂等: mq:consumed:stock-preoccupy:{fingerprint}

  order-stock-release:
    生产者: OrderServiceImpl
    消费者: StockReleaseConsumer（yuemo-product）
    消息体: Map<Long, Integer>（skuId→quantity）⚠️ 应改为 record
    发送方式: 普通消息
    幂等: mq:consumed:stock-release:{fingerprint}

  payment-callback:
    生产者: PaymentServiceImpl
    消费者: PaymentCallbackConsumer（yuemo-order）
    消息体: String（orderNo）⚠️ 应改为 record PaymentCallbackMessage
    发送方式: 普通消息
    幂等: mq:consumed:payment-callback:{orderNo}

  payment-refund:
    生产者: PaymentServiceImpl
    消费者: OrderRefundConsumer（yuemo-order）+ PaymentRefundConsumer（yuemo-payment）
    消息体: RefundMessage(orderId, userId, amount) ✅ record
    发送方式: 普通消息
    幂等: mq:consumed:order-refund:{orderId} + mq:consumed:payment-refund:{orderId}

  cart-sync:
    生产者: CartServiceImpl
    消费者: CartSyncConsumer（yuemo-cart，自身消费落库）
    消息体: CartSyncMessage（sealed interface + 6 record 变体）✅ record
    发送方式: 普通消息
    幂等: INSERT ... ON DUPLICATE KEY UPDATE
    死信: CartSyncDeadLetterConsumer（%DLQ%yuemo-cart-consumer）
```

```yaml
命名规则:
  - 全小写
  - 分隔符使用连字符(-)
  - {domain} 为业务域（order、payment、cart）
  - {event} 为事件描述（stock-preoccupy、callback、sync）
  - 禁止使用特殊字符和空格

常量化:
  - 所有 Topic 名称必须定义为 Java 常量，禁止硬编码字符串
  - 常量定义在各模块的 constant 包下（如 CartConstants.CART_SYNC_TOPIC）
  - 常量命名: {BIZ}_TOPIC，如 CART_SYNC_TOPIC = "cart-sync"
```

---

## 2. Consumer Group 命名

```
格式: {module}-consumer 或 {module}-{biz}-consumer

当前 Consumer Group:
  yuemo-cart-consumer                    — 购物车同步消费者（cart-sync）
  yuemo-cart-consumer-dlq                — 购物车死信消费者（%DLQ%yuemo-cart-consumer）
  yuemo-order-consumer                   — 订单支付回调消费者（payment-callback）
  yuemo-order-refund-consumer            — 订单退款通知消费者（payment-refund）
  yuemo-payment-consumer                 — 支付退款消费者（payment-refund）
  yuemo-product-consumer                 — 库存预占消费者（order-stock-preoccupy）
  yuemo-product-stock-release-consumer   — 库存释放消费者（order-stock-release）
```

```yaml
规则:
  - Consumer Group 与模块对应
  - 一个 Consumer Group 对应一个消费逻辑
  - 禁止多个 Consumer Group 消费同一 Topic 做重复逻辑

常量化:
  - 所有 Consumer Group 名称必须定义为 Java 常量
  - 常量命名: {BIZ}_CONSUMER_GROUP，如 CART_CONSUMER_GROUP = "yuemo-cart-consumer"
```

---

## 3. 消息设计

### 3.1 消息体

```yaml
必须:
  - 消息体使用 Java record（不可变）
  - 字段名使用 camelCase
  - 包含业务标识字段（如 orderNo、userId、skuId）
  - 消息体独立定义在 mq/ 或 dto/ 包

技术债务:
  payment-callback Topic 当前发送 String（orderNo），不符合 record 规范
  → 应改为 record PaymentCallbackMessage(String orderNo)，消费者同步修改

序列化:
  - 统一使用 JSON 序列化（Spring Boot 默认 Jackson）
  - 多态消息使用 @JsonTypeInfo + @JsonSubTypes（如 CartSyncMessage）
  - 消费者接收类型必须与生产者发送类型一致

禁止:
  - 消息体包含 Entity（避免序列化问题和字段耦合）
  - 消息体过大（> 1MB，超限用外部存储传递引用）
  - 使用 Java 序列化（用 JSON）
```

### 3.2 项目消息体示例

```java
// 购物车同步消息（密封接口 + 多态变体，配合策略模式分发）
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CartSyncMessage.Add.class, name = "ADD"),
    @JsonSubTypes.Type(value = CartSyncMessage.Update.class, name = "UPDATE"),
    @JsonSubTypes.Type(value = CartSyncMessage.Remove.class, name = "REMOVE"),
    @JsonSubTypes.Type(value = CartSyncMessage.SelectAll.class, name = "SELECT_ALL"),
    @JsonSubTypes.Type(value = CartSyncMessage.ClearSelected.class, name = "CLEAR_SELECTED"),
    @JsonSubTypes.Type(value = CartSyncMessage.ClearAll.class, name = "CLEAR_ALL")
})
public sealed interface CartSyncMessage
        permits CartSyncMessage.Add, CartSyncMessage.Update, CartSyncMessage.Remove,
        CartSyncMessage.SelectAll, CartSyncMessage.ClearSelected, CartSyncMessage.ClearAll {
    Long userId();
    record Add(Long userId, Long skuId, Long productId, Integer quantity, Boolean selected) implements CartSyncMessage {}
    record Update(Long userId, Long skuId, Integer quantity, Boolean selected) implements CartSyncMessage {}
    record Remove(Long userId, Long skuId) implements CartSyncMessage {}
    record SelectAll(Long userId, Boolean selected) implements CartSyncMessage {}
    record ClearSelected(Long userId) implements CartSyncMessage {}
    record ClearAll(Long userId) implements CartSyncMessage {}
}

// 退款消息
public record RefundMessage(Long orderId, Long userId, BigDecimal amount) {}
```

---

## 4. 消息幂等

```yaml
必须:
  - 所有消费者必须支持幂等消费
  - 幂等手段: Redis setIfAbsent + 业务唯一键
  - 统一幂等 Key 格式: mq:consumed:{topic}:{msgId}，TTL 10min
  - 处理失败时删除幂等 Key 以允许重试
  - 支付回调接口幂等（非消费者层面）: Redis payment:callback:{paymentNo} 5min TTL
  - 购物车同步幂等: INSERT ... ON DUPLICATE KEY UPDATE
  - 库存扣减幂等: UPDATE ... WHERE stock >= quantity（行级锁）

禁止:
  - 消费者直接处理而不做幂等判断
  - 依赖消息不重复的假设
```

---

## 5. 事务消息

```yaml
使用场景: 订单创建 → 库存预占（order-stock-preoccupy）

实现方式:
  1. OrderServiceImpl 发送事务消息（半消息），通过 Message header 传递 orderId:
     MessageBuilder.withPayload(skuStockMap).setHeader("orderId", order.getId()).build()
  2. OrderStockTransactionListener.executeLocalTransaction: 从 header 取 orderId，检查订单是否创建成功 → COMMIT/ROLLBACK
  3. OrderStockTransactionListener.checkLocalTransaction: 回查订单状态 → COMMIT/ROLLBACK
  4. COMMIT 后 StockPreoccupyConsumer 消费，执行库存扣减

规则:
  - 事务消息用于保证 DB 操作与 MQ 发送的原子性
  - TransactionListener 的 executeLocalTransaction 必须幂等
  - checkLocalTransaction 用于处理 executeLocalTransaction 超时的情况
```

---

## 6. 消费规范

```yaml
必须:
  - 可重试的临时错误（网络/锁/超时）→ 抛出异常触发 RocketMQ 重试
  - 不可重试的业务错误（数据不存在/格式错误）→ 捕获异常记录日志，不抛出
  - 消费失败记录到死信表或日志，不丢失
  - 消费逻辑幂等
  - 消费耗时 < 5s（避免超时重试）
  - 配置 maxReconsumeTimes 限制重试次数（如 CartSyncConsumer 设置为 5）

禁止:
  - 消费者中直接操作多个模块的数据库
  - 消费者中调用不幂等的外部接口
  - 消费者事务中包含 MQ 发送（事务内发 MQ 失败导致回滚不可控）
  - 消费者中阻塞等待外部资源

允许:
  - 消费者操作本模块多表时使用 @Transactional（保证本模块数据一致性）
  - 但事务范围应尽量小，耗时 < 5s
```

### 6.2 消息发送规范

```yaml
必须:
  - 使用 RocketMQTemplate 发送（禁止直接 new DefaultMQProducer）
  - 普通消息: rocketMQTemplate.convertAndSend(topic, message)
  - 事务消息: rocketMQTemplate.sendMessageInTransaction(topic, message, arg)
  - 发送超时: 配置 send-message-timeout（当前 3000ms）
  - 发送失败: 记录日志 + 抛出 BusinessException（不静默吞掉）

禁止:
  - 在 @Transactional 方法中发送普通消息（事务未提交时消息已发出，回滚后消息无法撤回）
    → 如需事务内发消息，使用事务消息
```

---

## 7. 重试与死信

```yaml
重试:
  - 消费失败自动重试（RocketMQ 默认 16 次，可通过 maxReconsumeTimes 配置）
  - 项目配置: CartSyncConsumer 设置 maxReconsumeTimes = 5
  - 重试间隔逐步递增（10s → 30s → 1min → 2min → ... → 2h）
  - 超过重试次数进入死信队列 %DLQ%{ConsumerGroup}

死信处理:
  - 定期查看死信队列
  - 死信消息记录到数据库或日志系统
  - 人工处理或通过管理接口重新投递
```

---

## 8. 禁止事项汇总

```yaml
禁止:
  - Topic/ConsumerGroup 随意命名
  - Topic/ConsumerGroup 硬编码字符串（必须定义为常量）
  - 消息体不带业务标识
  - 消费者不做幂等
  - 消费者中跨模块操作数据库
  - 消费者中阻塞等待
  - 消费者事务中包含 MQ 发送
  - 消息体超过 1MB
  - 在事务消息的 executeLocalTransaction 中做远程调用
  - 直接 new DefaultMQProducer（应使用 RocketMQTemplate）
  - 在 @Transactional 方法中发送普通消息（应使用事务消息）
```
