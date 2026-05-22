# 扩展性治理规则

> 让 AI Agent 默认面向扩展设计，而非面向当前功能设计。对高频扩展模块强制使用策略模式、工厂模式、SPI 机制和插件化架构。
> 设计模式的详细模板和禁止写法见 `.harness/rules/design-pattern-governance.md`。
> 本文件仅规定"哪些模块必须面向扩展设计"和"当前实现状态"。

---

## 1. 核心原则

```yaml
面向扩展设计 > 面向当前功能设计:
  - 新增功能通过新增类实现（而非修改现有类）
  - 新增行为无需修改原有代码（OCP）
  - 通过 @Component 自动注册（无需手动维护列表）
  - 删除功能只需删除对应的 @Component（不影响其他）

反模式:
  - "目前只有 2 种，先用 if-else，以后多了再改"  ← 以后永远不会改
  - "这个不太可能扩展" ← 业务永远在变化
  - "策略模式太复杂，switch 就行" ← 3 个 case 的 switch 已经违反规则
```

---

## 2. 高扩展模块清单

> 每个模块标注当前实现状态。设计模式的实现模板见 `.harness/rules/design-pattern-governance.md`。

### 2.1 支付系统 ✅ 已实现策略模式

```yaml
模块: yuemo-payment
当前: payType: 1-微信 2-支付宝 3-平台余额
扩展可能:
  - 银行卡支付
  - 花呗分期
  - 京东白条
  - 银联云闪付
  - 数字货币
  - 第三方钱包

已实现:
  接口: PayTypeHandler { int payType(); Payment doPay(Long userId, Payment payment); }
  工厂: PayTypeHandlerFactory（Spring List 注入 + Collectors.toMap）
  实现: WechatPayHandler / AlipayPayHandler / BalancePayHandler
  注册: handlers.stream().collect(Collectors.toMap(PayTypeHandler::payType, identity()))
  调用: payTypeHandlerFactory.get(payType).doPay(userId, payment)

待改进:
  - verifySign 职责下沉到 PayTypeHandler 接口（当前硬编码在 PaymentServiceImpl）
```

### 2.2 MQ 事件处理 ✅ 已实现策略模式

```yaml
模块: yuemo-cart（CartSyncConsumer）
当前:
  - cart-sync: 6 种 action（Add/Update/Remove/SelectAll/ClearSelected/ClearAll）
  - order-stock-preoccupy: 库存预占
  - order-stock-release: 库存释放
  - payment-callback: 支付回调
  - payment-refund: 退款通知
扩展可能:
  - 每种 Topic 的 action 都可能增加
  - 新业务必然引入新 Topic

已实现（标准范例）:
  消息类型: sealed interface CartSyncMessage permits Add, Update, Remove, ...
  多态反序列化: @JsonTypeInfo + @JsonSubTypes（Jackson）
  Handler 接口: CartActionHandler<T extends CartSyncMessage> { Class<T> messageType(); void handle(T msg); }
  注册: handlers.stream().collect(Collectors.toMap(CartActionHandler::messageType, identity()))
  分发: handlerMap.get(msg.getClass()).handle(msg)
  实现: AddCartHandler / UpdateCartHandler / RemoveCartHandler / SelectAllCartHandler / ClearSelectedCartHandler / ClearAllCartHandler
  扩展新动作: 新增 record 子类型 + 新增 Handler @Component，零修改原有代码
```

### 2.3 状态流转 ⚠️ 守卫模式（演进方向：状态模式）

```yaml
模块: yuemo-order
当前状态: 待支付→已支付→已发货→已完成→已取消
扩展可能:
  - 增加"部分退款"状态
  - 增加"换货中"状态
  - 增加"已评价"状态
  - 增加"售后处理中"状态

当前实现:
  模式: ensureStatus() 守卫方法（充血模型）
  示例: order.pay() → ensureStatus(UNPAID) → setStatus(PAID)
  优点: 简单直观，状态转换逻辑在 Entity 内
  缺点: 状态转换逻辑分散在各方法中，无集中状态转换表

演进方向:
  状态模式（OrderState 接口 + 各状态实现 + OrderStateFactory）
  详见 .harness/rules/design-pattern-governance.md 第3节

已知差距:
  - refundOrder() 绕过 Order 实体守卫直接 setStatus()
  - 无 REFUNDED 状态的守卫方法
  - 状态增多时 ensureStatus() 会膨胀为大型 if-else

触发迁移: 新增"部分退款"/"售后处理中"等状态时
```

### 2.4 营销规则 ❌ 未实现（实现时必须用策略模式）

```yaml
模块: yuemo-promotion
当前: CouponType 枚举（1-满减 2-折扣 3-立减），抵扣计算未实现
扩展可能:
  - 秒杀
  - 拼团
  - 砍价
  - 满赠
  - 新人专享
  - 会员折扣
  - 积分抵扣

实现时必须:
  接口: CouponTypeHandler { int couponType(); BigDecimal calculate(BigDecimal price, Coupon coupon); }
  工厂: CouponTypeHandlerFactory（Spring List 注入 + Collectors.toMap）
  实现: ThresholdDiscountHandler / DiscountHandler / FixedDiscountHandler
  调用: couponTypeHandlerFactory.get(coupon.getType()).calculate(price, coupon)

禁止: if-else / switch-case 按 couponType 分发计算逻辑
```

### 2.5 权限系统 ❌ 硬编码（演进方向：权限处理器+责任链）

```yaml
模块: yuemo-gateway
当前: GatewayAuthFilter 硬编码 if-else（仅支持 ADMIN 单角色）
扩展可能:
  - 多角色（ADMIN/OPERATOR/AUDITOR/MERCHANT）
  - 细粒度权限（商品管理/订单管理/用户管理）
  - 动态权限（运行时配置）

当前实现:
  if (path.startsWith("/api/admin/")) { if (!"ADMIN".equals(role)) → 403 }

演进方向:
  PermissionHandler 接口 + 责任链
  详见 .harness/rules/design-pattern-governance.md 第4节

触发迁移: 新增 OPERATOR/AUDITOR 等角色时
```

### 2.6 通知系统 ❌ 未实现（实现时必须用策略模式）

```yaml
模块: 全局（如有需求）
扩展可能:
  - 短信通知
  - 邮件通知
  - 站内信
  - App Push
  - 微信模板消息

实现时必须:
  接口: NotificationHandler { String channel(); void send(Notification notification); }
  工厂: NotificationHandlerFactory（Spring List 注入 + Collectors.toMap）
  调用: notificationHandlerFactory.get(channel).send(notification)

禁止: if-else / switch-case 按 channel 分发发送逻辑
```

---

## 3. 强制架构要求

> 设计模式的详细模板、标准实现和禁止写法见 `.harness/rules/design-pattern-governance.md`。
> 本节仅列出扩展性层面的强制规则。

### 3.1 必须使用的模式

```yaml
高扩展模块必须:
  - 策略模式: 每种行为一个 Handler 类
  - 工厂模式: Spring 自动装配所有 Handler（独立 Factory 类，禁止内联在 ServiceImpl 中）
  - Handler 注册机制: @Component 扫描 + List<Handler> 注入 + Collectors.toMap 自动注册
  - 接口隔离: Handler 接口方法最少化

Handler 命名规范:
  - 接口: {业务领域}Handler（如 PayTypeHandler、CartActionHandler）
  - 实现: {具体类型}Handler（如 WechatPayHandler、AddCartHandler）
  - 工厂: {业务领域}HandlerFactory（如 PayTypeHandlerFactory）

注册方式:
  允许: Spring List<Handler> 注入 + Collectors.toMap(Handler::keyMethod, identity()) 自动注册
  禁止: 手动 new HashMap<>() + put() 逐个注册（容易遗漏）

禁止:
  - switch-case 做行为分发
  - if-else 链做行为分发
  - 巨型 Dispatcher 类（所有逻辑堆在一个类里）
  - Handler 注册逻辑内联在 ServiceImpl 构造器中（必须抽取独立 Factory 类）
```

### 3.2 SPI 机制

```yaml
Java SPI 适用场景:
  - 跨模块的扩展点（如支付模块提供 SPI，其他模块实现）
  - 需要第三方扩展的能力

Spring @Component 自动装配（当前项目适用）:
  - 同一模块内的策略变体
  - 通过 List<HandlerInterface> 自动收集所有实现

何时使用 SPI 而非 @Component:
  - 扩展点跨 JAR 包 → SPI
  - 扩展点在当前模块内 → @Component + List<T> 自动装配
```

---

## 4. 非高扩展模块

```yaml
仅以下场景可豁免扩展性设计:
  - 语言/框架层面的固定规则（如"Java 构造器必须与类名一致"）
  - 至多 2 种情况且 6 个月内无扩展计划的简单判断
  - 一次性任务（如数据迁移脚本）
  - 纯工具方法（如日期格式化）

警告: 判断"不会扩展"时务必保守。历史上"不太可能扩展"的判断 90% 是错的。
      不确定时，优先面向扩展设计。
```

---

## 5. 高扩展模块识别标准

AI Agent 开发新功能时，满足以下**任一**条件即为高扩展模块：

```yaml
识别标准:
  1. 存在 type/kind/action 等多态字段
  2. 业务方明确表示未来会扩展（如"后续要加XX"）
  3. 同行业产品普遍支持多种类型（如支付方式、通知渠道）
  4. 当前已有 ≥ 3 种变体
  5. 变体逻辑独立、可封装

识别后的行动:
  - 查阅 .harness/rules/design-pattern-governance.md 选择合适的模式
  - 按标准模板实现 Handler + Factory
  - 在本文件第2节新增模块条目
```

---

## 6. 扩展性自检清单

AI Agent 生成代码前必须检查：

```yaml
1. 这段代码处理的业务未来会扩展吗？
   - 是 → 使用策略模式 / 状态模式
   - 不确定 → 使用策略模式（宁可多设计，不可欠设计）

2. 新增一种类型/行为需要改几个文件？
   - 只新增 1 个 @Component → ✅ 符合 OCP
   - 需要修改原有类 → ❌ 违反 OCP，重新设计

3. 删除一种类型/行为需要改几个文件？
   - 只删除 1 个 @Component → ✅ 可插拔
   - 需要修改 Dispatcher → ❌ 耦合，重新设计

4. 这个 Handler 可以独立测试吗？
   - 是 → ✅ 设计合格
   - 否 → ❌ 耦合，重新设计
```

---

## 7. 与其他治理文件的联动

```yaml
本规则与以下文件联动:
  - .harness/rules/design-pattern-governance.md: 设计模式的详细模板和禁止写法（本文件引用，不重复）
  - .harness/memory/anti-patterns.md: AP001 switch-case / AP002 if-else 链（已修复的反模式）
  - .harness/memory/decisions.md: D010 充血模型 / D011 策略模式（技术决策记录）
  - .harness/rules/architecture-governance.md: 分层约束（代码放在哪一层）
  - .harness/constraints/payment.md: 支付安全约束（支付模块的硬红线）

违反扩展性规则的后果:
  - 短期: 功能可用但增加技术债务
  - 中期: 新增功能成本递增（每次都要理解和修改原有逻辑）
  - 长期: 代码无法微服务拆分，需要全面重写
```
